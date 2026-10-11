package z.caAuth.account;

import java.io.File;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountRepository {
    private final File dbFile;
    private final Logger logger;
    private final SecureRandom random = new SecureRandom();
    private Connection connection;

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public AccountRepository(File dataFolder, Logger logger) {
        this.dbFile = new File(dataFolder, "accounts.db");
        this.logger = logger;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            dbFile.getParentFile().mkdirs();
            createTables();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при инициализации базы данных", e);
        }
    }

    private synchronized void createTables() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS accounts (
                player_uuid TEXT PRIMARY KEY,
                bound INTEGER DEFAULT 0,
                binding_type TEXT,
                binding_id TEXT,
                pending_code TEXT,
                last_confirmed_at INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """;

        String createTable2SQL = """
            CREATE TABLE IF NOT EXISTS activerole (
                player_uuid TEXT PRIMARY KEY,
                overridden INTEGER DEFAULT 0
            )
        """;

        String createTable3SQL = """
            CREATE TABLE IF NOT EXISTS twink_invites (
                twink_name TEXT PRIMARY KEY,
                real_name TEXT NOT NULL,
                owner_uuid TEXT NOT NULL,
                code TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """;

        String createTable4SQL = """
            CREATE TABLE IF NOT EXISTS skin_profiles (
                player_uuid TEXT NOT NULL,
                slot INTEGER NOT NULL,
                source TEXT NOT NULL,
                input TEXT NOT NULL,
                skin_id TEXT NOT NULL,
                skin_type TEXT NOT NULL,
                variant TEXT NOT NULL,
                texture_value TEXT NOT NULL,
                texture_signature TEXT,
                updated_at INTEGER DEFAULT (strftime('%s', 'now')),
                PRIMARY KEY (player_uuid, slot)
            )
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createTable2SQL);
            stmt.execute(createTable3SQL);
            stmt.execute(createTable4SQL);
        }

        addColumnIfMissing("last_confirmed_at INTEGER DEFAULT 0");
        addColumnIfMissing("last_confirmed_ip TEXT");
        addColumnIfMissing("player_name TEXT");
        addColumnIfMissing("pending_code_created_at INTEGER DEFAULT 0");
        addColumnIfMissing("owner_uuid TEXT");

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_binding_id ON accounts(binding_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_binding_type ON accounts(binding_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner_uuid ON accounts(owner_uuid)");
        }
    }

    private void addColumnIfMissing(String columnDefinition) {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("ALTER TABLE accounts ADD COLUMN " + columnDefinition);
        } catch (SQLException ignored) {
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        return connection;
    }

    private synchronized <T> T queryOne(String sql, Binder binder, RowMapper<T> mapper, T fallback, String error) {
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            binder.bind(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapper.map(rs) : fallback;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, error, e);
            return fallback;
        }
    }

    private synchronized <T> List<T> queryList(String sql, Binder binder, RowMapper<T> mapper, String error) {
        List<T> result = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            binder.bind(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, error, e);
        }
        return result;
    }

    private synchronized void update(String sql, Binder binder, String error) {
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            binder.bind(pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, error, e);
        }
    }

    private static Binder uuid(UUID playerUUID) {
        return pstmt -> pstmt.setString(1, playerUUID.toString());
    }

    private static UUID readUuid(ResultSet rs) throws SQLException {
        return UUID.fromString(rs.getString("player_uuid"));
    }

    public boolean isBound(UUID playerUUID) {
        return queryOne("SELECT bound FROM accounts WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getInt("bound") == 1, false,
                "Ошибка при проверке привязки");
    }

    public boolean isActiveRole(UUID playerUUID) {
        return queryOne("SELECT overridden FROM activerole WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getInt("overridden") == 1, false,
                "Ошибка при проверке overridden активной роли");
    }

    public void setOverridden(UUID playerUUID, boolean overridden) {
        update("INSERT OR REPLACE INTO activerole (player_uuid, overridden) VALUES (?, ?)", pstmt -> {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setBoolean(2, overridden);
        }, "Ошибка при записи overridden активной роли");
    }

    public synchronized Map<UUID, String> getBindingsByType(String type) {
        String sql = "SELECT player_uuid, binding_id FROM accounts WHERE bound = 1 AND owner_uuid IS NULL AND binding_type = ? AND binding_id IS NOT NULL";
        Map<UUID, String> result = new LinkedHashMap<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.put(readUuid(rs), rs.getString("binding_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Ошибка при получении привязок по типу", e);
        }
        return result;
    }

    public synchronized List<String> getRegisteredPlayerNames() {
        String sql = "SELECT player_name FROM accounts WHERE bound = 1 AND player_name IS NOT NULL AND player_name != '' ORDER BY player_name COLLATE NOCASE";
        List<String> result = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Ошибка при получении ников зарегистрированных игроков", e);
        }
        return result;
    }

    public void bind(UUID playerUUID, String bindingType, String bindingId) {
        String sql = """
            INSERT INTO accounts (player_uuid, bound, binding_type, binding_id, updated_at)
            VALUES (?, 1, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(player_uuid) DO UPDATE SET
                bound = 1,
                binding_type = excluded.binding_type,
                binding_id = excluded.binding_id,
                owner_uuid = NULL,
                pending_code = NULL,
                updated_at = excluded.updated_at
        """;
        update(sql, pstmt -> {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, bindingType);
            pstmt.setString(3, bindingId);
        }, "Ошибка при привязке " + bindingType);
    }

    public String getBindingType(UUID playerUUID) {
        return queryOne("SELECT binding_type FROM accounts WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getString("binding_type"), null,
                "Ошибка при получении типа привязки");
    }

    public String getBindingId(UUID playerUUID) {
        return queryOne("SELECT binding_id FROM accounts WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getString("binding_id"), null,
                "Ошибка при получении ID привязки");
    }

    public UUID getPlayerByBinding(String bindingType, String bindingId) {
        return queryOne("SELECT player_uuid FROM accounts WHERE owner_uuid IS NULL AND binding_type = ? AND binding_id = ?", pstmt -> {
            pstmt.setString(1, bindingType);
            pstmt.setString(2, bindingId);
        }, AccountRepository::readUuid, null, "Ошибка при поиске игрока по ID " + bindingType);
    }

    public void unbind(UUID playerUUID) {
        update("DELETE FROM accounts WHERE player_uuid = ?", uuid(playerUUID),
                "Ошибка при отвязке аккаунта");
    }

    public synchronized String generateVerificationCode(UUID playerUUID, String playerName, long notBeforeEpochSeconds) {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (findActiveCodeOwner(code, notBeforeEpochSeconds) != null);

        String finalCode = code;
        String sql = """
            INSERT INTO accounts (player_uuid, pending_code, pending_code_created_at, player_name, updated_at)
            VALUES (?, ?, strftime('%s', 'now'), ?, strftime('%s', 'now'))
            ON CONFLICT(player_uuid) DO UPDATE SET
                pending_code = excluded.pending_code,
                pending_code_created_at = excluded.pending_code_created_at,
                player_name = COALESCE(excluded.player_name, accounts.player_name),
                updated_at = excluded.updated_at
        """;
        update(sql, pstmt -> {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, finalCode);
            pstmt.setString(3, playerName);
        }, "Ошибка при генерации кода верификации");
        return code;
    }

    public synchronized UUID consumePendingCode(String code, long notBeforeEpochSeconds) {
        UUID owner = findActiveCodeOwner(code, notBeforeEpochSeconds);
        if (owner != null) {
            update("UPDATE accounts SET pending_code = NULL, updated_at = strftime('%s', 'now') WHERE player_uuid = ?",
                    uuid(owner), "Ошибка при погашении кода верификации");
        }
        return owner;
    }

    private UUID findActiveCodeOwner(String code, long notBeforeEpochSeconds) {
        return queryOne("SELECT player_uuid FROM accounts WHERE pending_code = ? AND pending_code_created_at >= ? LIMIT 1",
                pstmt -> {
                    pstmt.setString(1, code);
                    pstmt.setLong(2, notBeforeEpochSeconds);
                }, AccountRepository::readUuid, null, "Ошибка при поиске игрока по коду верификации");
    }

    public void setPlayerName(UUID playerUUID, String playerName) {
        String sql = """
            INSERT INTO accounts (player_uuid, player_name, updated_at)
            VALUES (?, ?, strftime('%s', 'now'))
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                updated_at = excluded.updated_at
        """;
        update(sql, pstmt -> {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerName);
        }, "Ошибка при сохранении ника игрока");
    }

    public String getPlayerName(UUID playerUUID) {
        return queryOne("SELECT player_name FROM accounts WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getString("player_name"), null,
                "Ошибка при получении ника игрока");
    }

    public long getLastConfirmedAt(UUID playerUUID) {
        return queryOne("SELECT last_confirmed_at FROM accounts WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getLong("last_confirmed_at"), 0L,
                "Ошибка при получении времени подтверждения входа");
    }

    public void setLastConfirmedAt(UUID playerUUID, long epochSeconds) {
        update("UPDATE accounts SET last_confirmed_at = ?, updated_at = strftime('%s', 'now') WHERE player_uuid = ?", pstmt -> {
            pstmt.setLong(1, epochSeconds);
            pstmt.setString(2, playerUUID.toString());
        }, "Ошибка при сохранении времени подтверждения входа");
    }

    public String getLastConfirmedIp(UUID playerUUID) {
        return queryOne("SELECT last_confirmed_ip FROM accounts WHERE player_uuid = ?",
                uuid(playerUUID), rs -> rs.getString("last_confirmed_ip"), null,
                "Ошибка при получении IP подтверждения входа");
    }

    public void setLastConfirmedIp(UUID playerUUID, String ip) {
        update("UPDATE accounts SET last_confirmed_ip = ?, updated_at = strftime('%s', 'now') WHERE player_uuid = ?", pstmt -> {
            pstmt.setString(1, ip);
            pstmt.setString(2, playerUUID.toString());
        }, "Ошибка при сохранении IP подтверждения входа");
    }

    public record TwinkRow(UUID uuid, String name) {
    }

    public record Invite(String twinkName, String realName, UUID owner, String code, long createdAt) {
    }

    private static Invite readInvite(ResultSet rs) throws SQLException {
        return new Invite(rs.getString("twink_name"), rs.getString("real_name"), UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("code"), rs.getLong("created_at"));
    }

    public void bindTwink(UUID twinkUUID, UUID ownerUUID, String bindingType, String bindingId, String twinkName) {
        String sql = """
            INSERT INTO accounts (player_uuid, bound, binding_type, binding_id, owner_uuid, player_name, updated_at)
            VALUES (?, 1, ?, ?, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(player_uuid) DO UPDATE SET
                bound = 1,
                binding_type = excluded.binding_type,
                binding_id = excluded.binding_id,
                owner_uuid = excluded.owner_uuid,
                player_name = excluded.player_name,
                pending_code = NULL,
                updated_at = excluded.updated_at
        """;
        update(sql, pstmt -> {
            pstmt.setString(1, twinkUUID.toString());
            pstmt.setString(2, bindingType);
            pstmt.setString(3, bindingId);
            pstmt.setString(4, ownerUUID.toString());
            pstmt.setString(5, twinkName);
        }, "Ошибка при привязке твинка");
    }

    public UUID getBoundPlayerByName(String playerName) {
        return queryOne("SELECT player_uuid FROM accounts WHERE bound = 1 AND player_name = ? COLLATE NOCASE LIMIT 1",
                pstmt -> pstmt.setString(1, playerName), AccountRepository::readUuid, null,
                "Ошибка при поиске привязанного игрока по нику");
    }

    public UUID getOwner(UUID playerUUID) {
        return queryOne("SELECT owner_uuid FROM accounts WHERE player_uuid = ? AND owner_uuid IS NOT NULL",
                uuid(playerUUID), rs -> UUID.fromString(rs.getString("owner_uuid")), null,
                "Ошибка при получении владельца твинка");
    }

    public List<TwinkRow> getTwinks(UUID ownerUUID) {
        return queryList("SELECT player_uuid, player_name FROM accounts WHERE owner_uuid = ? AND bound = 1 ORDER BY updated_at",
                uuid(ownerUUID), rs -> new TwinkRow(readUuid(rs), rs.getString("player_name")),
                "Ошибка при получении твинков");
    }

    public void deleteTwinks(UUID ownerUUID) {
        update("DELETE FROM accounts WHERE owner_uuid = ?", uuid(ownerUUID), "Ошибка при удалении твинков");
    }

    public void createInvite(String twinkName, String realName, UUID ownerUUID, String code, long createdAt) {
        update("INSERT OR REPLACE INTO twink_invites (twink_name, real_name, owner_uuid, code, created_at) VALUES (?, ?, ?, ?, ?)", pstmt -> {
            pstmt.setString(1, twinkName);
            pstmt.setString(2, realName);
            pstmt.setString(3, ownerUUID.toString());
            pstmt.setString(4, code);
            pstmt.setLong(5, createdAt);
        }, "Ошибка при создании приглашения твинка");
    }

    public Invite findInvite(String twinkName, long notBeforeEpochSeconds) {
        return queryOne("SELECT twink_name, real_name, owner_uuid, code, created_at FROM twink_invites WHERE twink_name = ? AND created_at >= ?", pstmt -> {
            pstmt.setString(1, twinkName);
            pstmt.setLong(2, notBeforeEpochSeconds);
        }, AccountRepository::readInvite, null, "Ошибка при поиске приглашения твинка");
    }

    public List<Invite> getInvites(UUID ownerUUID, long notBeforeEpochSeconds) {
        return queryList("SELECT twink_name, real_name, owner_uuid, code, created_at FROM twink_invites WHERE owner_uuid = ? AND created_at >= ? ORDER BY created_at", pstmt -> {
            pstmt.setString(1, ownerUUID.toString());
            pstmt.setLong(2, notBeforeEpochSeconds);
        }, AccountRepository::readInvite, "Ошибка при получении приглашений твинков");
    }

    public boolean isInviteCodeTaken(String code, long notBeforeEpochSeconds) {
        return queryOne("SELECT 1 FROM twink_invites WHERE code = ? AND created_at >= ? LIMIT 1", pstmt -> {
            pstmt.setString(1, code);
            pstmt.setLong(2, notBeforeEpochSeconds);
        }, rs -> true, false, "Ошибка при проверке кода приглашения");
    }

    public void deleteInvite(String twinkName) {
        update("DELETE FROM twink_invites WHERE twink_name = ?", pstmt -> pstmt.setString(1, twinkName),
                "Ошибка при удалении приглашения твинка");
    }

    public void deleteInvites(UUID ownerUUID) {
        update("DELETE FROM twink_invites WHERE owner_uuid = ?", uuid(ownerUUID),
                "Ошибка при удалении приглашений твинков");
    }

    public synchronized List<SkinProfile> getSkinProfiles(UUID playerUUID) {
        return queryList("SELECT slot, source, input, skin_id, skin_type, variant, texture_value, texture_signature "
                        + "FROM skin_profiles WHERE player_uuid = ? ORDER BY slot",
                uuid(playerUUID), AccountRepository::readSkinProfile, "Ошибка при чтении скинов");
    }

    public SkinProfile getSkinProfile(UUID playerUUID, int slot) {
        return queryOne("SELECT slot, source, input, skin_id, skin_type, variant, texture_value, texture_signature "
                        + "FROM skin_profiles WHERE player_uuid = ? AND slot = ?", pstmt -> {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setInt(2, slot);
                }, AccountRepository::readSkinProfile, null, "Ошибка при чтении скина");
    }

    public void saveSkinProfile(UUID playerUUID, SkinProfile profile) {
        String sql = """
            INSERT INTO skin_profiles (player_uuid, slot, source, input, skin_id, skin_type, variant,
                texture_value, texture_signature, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(player_uuid, slot) DO UPDATE SET
                source = excluded.source,
                input = excluded.input,
                skin_id = excluded.skin_id,
                skin_type = excluded.skin_type,
                variant = excluded.variant,
                texture_value = excluded.texture_value,
                texture_signature = excluded.texture_signature,
                updated_at = excluded.updated_at
        """;
        update(sql, pstmt -> {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setInt(2, profile.slot());
            pstmt.setString(3, profile.source().name());
            pstmt.setString(4, profile.input());
            pstmt.setString(5, profile.skinId());
            pstmt.setString(6, profile.skinType());
            pstmt.setString(7, profile.variant());
            pstmt.setString(8, profile.textureValue());
            pstmt.setString(9, profile.textureSignature());
        }, "Ошибка при сохранении скина");
    }

    public void deleteSkinProfile(UUID playerUUID, int slot) {
        update("DELETE FROM skin_profiles WHERE player_uuid = ? AND slot = ?", pstmt -> {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setInt(2, slot);
        }, "Ошибка при удалении скина");
    }

    private static SkinProfile readSkinProfile(ResultSet rs) throws SQLException {
        return new SkinProfile(rs.getInt("slot"), SkinSource.valueOf(rs.getString("source")),
                rs.getString("input"), rs.getString("skin_id"), rs.getString("skin_type"),
                rs.getString("variant"), rs.getString("texture_value"), rs.getString("texture_signature"));
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Ошибка при закрытии соединения", e);
        }
    }
}
