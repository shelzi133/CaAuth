package z.caAuth.account;

import java.io.File;
import java.sql.*;
import java.util.Random;
import java.util.UUID;

 //sqlite
public class AccountStorage {
    private final File dbFile;
    private Connection connection;
    private final Random random = new Random();
    
    public AccountStorage(File dataFolder) {
        this.dbFile = new File(dataFolder, "accounts.db");
        initializeDatabase();
    }
    private void initializeDatabase() {
        try {
            dbFile.getParentFile().mkdirs();
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            createTables();
        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void createTables() throws SQLException {
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
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
        // Миграция для существующих БД, созданных до появления колонки last_confirmed_at
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE accounts ADD COLUMN last_confirmed_at INTEGER DEFAULT 0");
        } catch (SQLException ignored) {
            // колонка уже существует
        }
        String createIndexSQL1 = "CREATE INDEX IF NOT EXISTS idx_binding_id ON accounts(binding_id)";
        String createIndexSQL2 = "CREATE INDEX IF NOT EXISTS idx_binding_type ON accounts(binding_type)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL1);
            stmt.execute(createIndexSQL2);
        }
    }
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }
    
    public boolean isBound(UUID playerUUID) {
        String sql = "SELECT bound FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("bound") == 1;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при проверке привязки: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public void bindDiscord(UUID playerUUID, String discordId) {
        String sql = """
            INSERT OR REPLACE INTO accounts (player_uuid, bound, binding_type, binding_id, updated_at)
            VALUES (?, 1, 'discord', ?, strftime('%s', 'now'))
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при привязке Discord: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void bindTelegram(UUID playerUUID, String telegramId) {
        String sql = """
            INSERT OR REPLACE INTO accounts (player_uuid, bound, binding_type, binding_id, updated_at)
            VALUES (?, 1, 'telegram', ?, strftime('%s', 'now'))
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, telegramId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при привязке Telegram: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public String getBindingType(UUID playerUUID) {
        String sql = "SELECT binding_type FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("binding_type");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении типа привязки: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public String getBindingId(UUID playerUUID) {
        String sql = "SELECT binding_id FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("binding_id");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении ID привязки: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public void unbind(UUID playerUUID) {
        String sql = "DELETE FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при отвязке аккаунта: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public String generateVerificationCode(UUID playerUUID) {
        String code = String.format("%06d", random.nextInt(1000000));
        String sql = """
            INSERT OR REPLACE INTO accounts (player_uuid, pending_code, updated_at)
            VALUES (?, ?, strftime('%s', 'now'))
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при генерации кода верификации: " + e.getMessage());
            e.printStackTrace();
        }
        return code;
    }
    
    public boolean verifyCode(UUID playerUUID, String code) {
        String sql = "SELECT pending_code FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedCode = rs.getString("pending_code");
                if (storedCode != null && storedCode.equals(code)) {
                    String updateSQL = "UPDATE accounts SET pending_code = NULL, updated_at = strftime('%s', 'now') WHERE player_uuid = ?";
                    try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSQL)) {
                        updateStmt.setString(1, playerUUID.toString());
                        updateStmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при проверке кода верификации: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    public UUID getPlayerByDiscordId(String discordId) {
        String sql = "SELECT player_uuid FROM accounts WHERE binding_type = 'discord' AND binding_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("player_uuid"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске игрока по Discord ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


     public String getDiscordId(UUID playerUUID) {
         String sql = "SELECT binding_id FROM accounts WHERE player_uuid = ? AND binding_type = 'discord'";
         try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
             pstmt.setString(1, playerUUID.toString());
             ResultSet rs = pstmt.executeQuery();
             if (rs.next()) {
                 return rs.getString("binding_id");
             }
         } catch (SQLException e) {
             System.err.println("Ошибка при получении Discord ID: " + e.getMessage());
             e.printStackTrace();
         }
         return null;
     }

    public UUID getPlayerByTelegramId(String telegramId) {
        String sql = "SELECT player_uuid FROM accounts WHERE binding_type = 'telegram' AND binding_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, telegramId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("player_uuid"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске игрока по Telegram ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    public String getPendingCode(UUID playerUUID) {
        String sql = "SELECT pending_code FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("pending_code");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении кода верификации: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    public void setPendingCode(UUID playerUUID, String code) {
        String sql = """
            INSERT OR REPLACE INTO accounts (player_uuid, pending_code, updated_at)
            VALUES (?, ?, strftime('%s', 'now'))
        """;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при установке кода верификации: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public long getLastConfirmedAt(UUID playerUUID) {
        String sql = "SELECT last_confirmed_at FROM accounts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_confirmed_at");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении времени подтверждения входа: " + e.getMessage());
            e.printStackTrace();
        }
        return 0L;
    }

    public void setLastConfirmedAt(UUID playerUUID, long epochSeconds) {
        String sql = "UPDATE accounts SET last_confirmed_at = ?, updated_at = strftime('%s', 'now') WHERE player_uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, epochSeconds);
            pstmt.setString(2, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении времени подтверждения входа: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
