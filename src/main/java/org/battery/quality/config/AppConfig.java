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

    // Doris规则库配置
    private DorisRuleConfig dorisRule = new DorisRuleConfig();

    // Sink配置
    private SinkConfig sink = new SinkConfig();

    // Doris数据输出配置
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
        
        // Doris规则库配置
        configMap.put("doris.rule.url", dorisRule.getUrl());
        configMap.put("doris.rule.username", dorisRule.getUsername());
        configMap.put("doris.rule.password", dorisRule.getPassword());
        configMap.put("doris.rule.database", dorisRule.getDatabase());
        configMap.put("doris.rule.maxPoolSize", String.valueOf(dorisRule.getMaxPoolSize()));
        configMap.put("doris.rule.minPoolSize", String.valueOf(dorisRule.getMinPoolSize()));
        configMap.put("doris.rule.connectionTimeout", String.valueOf(dorisRule.getConnectionTimeout()));
        configMap.put("doris.rule.cacheRefreshInterval", String.valueOf(dorisRule.getCacheRefreshInterval()));
        
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
     * Doris规则库配置
     */
    @Data
    public static class DorisRuleConfig {
        private String url = "jdbc:mysql://localhost:9030/battery_quality?useSSL=false&serverTimezone=Asia/Shanghai";
        private String username = "root";
        private String password = "";
        private String database = "battery_quality";
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
        private String database = "battery_quality";
        private String table = "ods_data_with_issues";
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