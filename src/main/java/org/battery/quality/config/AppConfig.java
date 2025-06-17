package org.battery.quality.config;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 应用配置类
 */
@Data
public class AppConfig implements Serializable {
    // Kafka配置
    private KafkaConfig kafka = new KafkaConfig();
    
    // 处理配置
    private ProcessConfig process = new ProcessConfig();
    
    // MySQL配置
    private MySQLConfig mysql = new MySQLConfig();
    
    // Sink配置
    private SinkConfig sink = new SinkConfig();
    
    // Doris配置
    private DorisConfig doris = new DorisConfig();
    
    // Print配置
    private PrintConfig print = new PrintConfig();
    
    /**
     * 将配置转换为Map
     * 
     * @return 包含所有配置项的Map
     */
    public Map<String, String> toMap() {
        Map<String, String> configMap = new HashMap<>();
        
        // Kafka配置
        configMap.put("kafka.bootstrapServers", kafka.getBootstrapServers());
        configMap.put("kafka.topic", kafka.getTopic());
        configMap.put("kafka.groupId", kafka.getGroupId());
        configMap.put("kafka.autoOffsetReset", kafka.getAutoOffsetReset());
        
        // 处理配置
        configMap.put("process.parallelism", String.valueOf(process.getParallelism()));
        configMap.put("process.stateRetentionMinutes", String.valueOf(process.getStateRetentionMinutes()));
        configMap.put("process.checkpointInterval", String.valueOf(process.getCheckpointInterval()));
        
        // MySQL配置
        configMap.put("mysql.url", mysql.getUrl());
        configMap.put("mysql.username", mysql.getUsername());
        configMap.put("mysql.password", mysql.getPassword());
        configMap.put("mysql.maxPoolSize", String.valueOf(mysql.getMaxPoolSize()));
        configMap.put("mysql.minPoolSize", String.valueOf(mysql.getMinPoolSize()));
        configMap.put("mysql.connectionTimeout", String.valueOf(mysql.getConnectionTimeout()));
        configMap.put("mysql.cacheRefreshInterval", String.valueOf(mysql.getCacheRefreshInterval()));
        
        // Sink配置
        configMap.put("sink.type", sink.getType());
        
        // Doris配置
        configMap.put("doris.conn", doris.getConn());
        configMap.put("doris.user", doris.getUser());
        configMap.put("doris.passwd", doris.getPasswd());
        configMap.put("doris.database", doris.getDatabase());
        configMap.put("doris.table", doris.getTable());
        configMap.put("doris.batchSize", String.valueOf(doris.getBatchSize()));
        configMap.put("doris.batchIntervalMs", String.valueOf(doris.getBatchIntervalMs()));
        configMap.put("doris.maxRetries", String.valueOf(doris.getMaxRetries()));
        configMap.put("doris.maxBatchBytes", String.valueOf(doris.getMaxBatchBytes()));
        
        // Print配置
        configMap.put("print.identifier", print.getIdentifier());
        configMap.put("print.verbose", String.valueOf(print.isVerbose()));
        
        return configMap;
    }
    
    /**
     * Kafka配置
     */
    @Data
    public static class KafkaConfig {
        private String bootstrapServers = "localhost:9092";
        private String topic = "gb32960-data";
        private String groupId = "data-quality-group";
        private String autoOffsetReset = "latest";
    }
    
    /**
     * 处理配置
     */
    @Data
    public static class ProcessConfig {
        // 并行度
        private int parallelism = 4;
        
        // 状态保留时间（分钟）
        private int stateRetentionMinutes = 60;
        
        // 检查点间隔（毫秒）
        private long checkpointInterval = 60000;
    }
    
    /**
     * MySQL配置
     */
    @Data
    public static class MySQLConfig {
        private String url = "jdbc:mysql://localhost:3306/battery_quality?useSSL=false&serverTimezone=Asia/Shanghai";
        private String username = "root";
        private String password = "password";
        private int maxPoolSize = 10;
        private int minPoolSize = 2;
        private long connectionTimeout = 30000;
        // 缓存刷新间隔（秒）
        private long cacheRefreshInterval = 10;
    }
    
    /**
     * Sink配置
     */
    @Data
    public static class SinkConfig {
        // Sink类型：doris, print, both
        private String type = "doris";
    }
    
    /**
     * Doris配置
     */
    @Data
    public static class DorisConfig {
        private String conn = "localhost:8030";
        private String user = "root";
        private String passwd = "";
        private String database = "battery_data";
        private String table = "gb32960_data";
        private int batchSize = 1000;
        private int batchIntervalMs = 2000;
        private int maxRetries = Integer.MAX_VALUE;
        private long maxBatchBytes = 1024 * 1024 * 1024L; // 1GB
    }
    
    /**
     * Print配置
     */
    @Data
    public static class PrintConfig {
        private String identifier = "质量检查结果";
        private boolean verbose = false;
    }
} 