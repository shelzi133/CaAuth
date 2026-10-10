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

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createTable2SQL);
        }

        addColumnIfMissing("last_confirmed_at INTEGER DEFAULT 0");
        addColumnIfMissing("last_confirmed_ip TEXT");
        addColumnIfMissing("player_name TEXT");
        addColumnIfMissing("pending_code_created_at INTEGER DEFAULT 0");

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_binding_id ON accounts(binding_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_binding_type ON accounts(binding_type)");
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
        String sql = "SELECT player_uuid, binding_id FROM accounts WHERE bound = 1 AND binding_type = ? AND binding_id IS NOT NULL";
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
        return queryOne("SELECT player_uuid FROM accounts WHERE binding_type = ? AND binding_id = ?", pstmt -> {
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
