package org.battery.quality.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接管理器
 */
public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private DataSource dataSource;
    
    private DatabaseManager() {
        // 私有构造函数
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * 检查数据源是否已初始化
     * 
     * @return 数据源是否已初始化
     */
    public boolean isDataSourceInitialized() {
        return dataSource != null;
    }
    
    /**
     * 初始化数据源
     */
    public void initDataSource(AppConfig.MySQLConfig config) {
        try {
            LOGGER.info("初始化数据库连接池: {}", config.getUrl());
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
            hikariConfig.setMinimumIdle(config.getMinPoolSize());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(hikariConfig);
            LOGGER.info("数据库连接池初始化成功");
        } catch (Exception e) {
            LOGGER.error("初始化数据库连接池失败", e);
            throw new RuntimeException("初始化数据库连接池失败", e);
        }
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("数据源未初始化");
        }
        return dataSource.getConnection();
    }
    
    /**
     * 关闭连接池
     */
    public void closeDataSource() {
        if (dataSource instanceof HikariDataSource) {
            LOGGER.info("关闭数据库连接池");
            ((HikariDataSource) dataSource).close();
            dataSource = null;
        }
    }
} 