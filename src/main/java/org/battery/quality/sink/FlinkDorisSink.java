package org.battery.quality.sink;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisSink;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.util.Properties;

/**
 * Doris数据库Sink实现
 * 提供将处理后的数据写入Doris数据库的SinkFunction
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Slf4j
public class FlinkDorisSink implements Sink {
    
    // Doris连接配置
    private String feNodes;        // FE节点地址，如 "localhost:8030"
    private String username;       // 用户名
    private String password;       // 密码
    private String database;       // 数据库名
    private String tableName;      // 表名
    
    // 执行配置
    private Integer batchSize;          // 批处理大小
    private Long batchIntervalMs;       // 批处理间隔（毫秒）
    private Integer maxRetries;         // 最大重试次数
    private Long maxBatchBytes;         // 最大批处理字节数
    private Boolean enableDelete;       // 是否启用删除操作
    
    // StreamLoad属性
    @Getter(AccessLevel.PROTECTED)
    private final Properties streamLoadProps = new Properties();
    
    /**
     * 初始化StreamLoad属性
     */
    {
        streamLoadProps.setProperty("format", "json");
        streamLoadProps.setProperty("json_root", "$.data");
        streamLoadProps.setProperty("array-object", "true");
        streamLoadProps.setProperty("strip_outer_array", "true");
    }
    
    /**
     * 全参数构造函数
     */
    public FlinkDorisSink(String feNodes, String username, String password, String database, String tableName) {
        this.feNodes = feNodes;
        this.username = username;
        this.password = password;
        this.database = database;
        this.tableName = tableName;
    }

    @Override
    public SinkFunction<String> getSinkFunction(ParameterTool parameterTool) {
        // 从参数工具获取配置（如果成员变量未设置）
        String feNodes = this.feNodes != null ? this.feNodes : parameterTool.get("doris.conn");
        String username = this.username != null ? this.username : parameterTool.get("doris.user");
        String password = this.password != null ? this.password : parameterTool.get("doris.passwd");
        String database = this.database != null ? this.database : parameterTool.get("doris.database", "battery_data");
        String tableName = this.tableName != null ? this.tableName : parameterTool.get("doris.table", "gb32960_data_with_issues");
        String tableIdentifier = database + "." + tableName;
        
        // 获取执行配置
        int batchSize = this.batchSize != null ? this.batchSize : parameterTool.getInt("doris.batchSize", 1000);
        long batchIntervalMs = this.batchIntervalMs != null ? this.batchIntervalMs : parameterTool.getInt("doris.batchIntervalMs", 2000);
        int maxRetries = this.maxRetries != null ? this.maxRetries : parameterTool.getInt("doris.maxRetries", Integer.MAX_VALUE);
        long maxBatchBytes = this.maxBatchBytes != null ? this.maxBatchBytes : parameterTool.getLong("doris.maxBatchBytes", 1024 * 1024 * 1024L);
        boolean enableDelete = this.enableDelete != null ? this.enableDelete : false;
        
        log.info("配置DorisSink: 表={}, 连接={}", tableIdentifier, feNodes);
        
        // 创建并返回DorisSink
        return DorisSink.sink(
            new DorisExecutionOptions.Builder()
              .setBatchIntervalMs(batchIntervalMs)
              .setBatchSize(batchSize)
              .setEnableDelete(enableDelete)
              .setMaxRetries(maxRetries)
              .setMaxBatchBytes(maxBatchBytes)
              .setStreamLoadProp(streamLoadProps)
              .build(),
            new DorisOptions.Builder()
              .setFenodes(feNodes)
              .setUsername(username)
              .setPassword(password)
              .setTableIdentifier(tableIdentifier)
              .build()
        );
    }
    
    /**
     * 获取不带参数的SinkFunction
     */
    public SinkFunction<String> getSinkFunction() {
        return getSinkFunction(null);
    }
    
    /**
     * 添加StreamLoad属性
     */
    public FlinkDorisSink addStreamLoadProp(String key, String value) {
        this.streamLoadProps.setProperty(key, value);
        return this;
    }
} 