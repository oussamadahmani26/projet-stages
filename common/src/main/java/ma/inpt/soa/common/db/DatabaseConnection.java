package ma.inpt.soa.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import ma.inpt.soa.common.config.AppConfig;

/** Pool de connexions partagé par les DAO d'un service. */
public final class DatabaseConnection {
    private static HikariDataSource dataSource;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return pool().getConnection();
    }

    private static synchronized HikariDataSource pool() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(AppConfig.get("DB_URL",
                    "jdbc:mysql://localhost:3306/gestion_stages?useUnicode=true&characterEncoding=UTF-8"));
            config.setUsername(AppConfig.get("DB_USER", "root"));
            config.setPassword(AppConfig.get("DB_PASSWORD", ""));
            config.setMaximumPoolSize(AppConfig.getInt("DB_POOL_SIZE", 5));
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000);
            // Le service démarre même si la base n'est pas encore disponible.
            config.setInitializationFailTimeout(-1);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    public static synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
