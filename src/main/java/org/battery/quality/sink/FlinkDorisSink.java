package org.battery.quality.sink;

import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisSink;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Doris数据库Sink实现
 * 提供将处理后的数据写入Doris数据库的SinkFunction
 */
public class FlinkDorisSink implements Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(DorisSink.class);
    
    // 目标表名
    private final String targetTable;
    
    /**
     * 默认构造函数
     */
    public FlinkDorisSink() {
        this.targetTable = null;
    }
    
    /**
     * 指定目标表的构造函数
     * 
     * @param tableName 目标表名，可为null使用配置中的默认表
     */
    public FlinkDorisSink(String tableName) {
        this.targetTable = tableName;
    }

    @Override
    public SinkFunction<String> getSinkFunction(ParameterTool parameterTool) {
        // 准备StreamLoad参数
        Properties props = new Properties();
        props.setProperty("format", "json");
        props.setProperty("array-object","true");
        props.setProperty("strip_outer_array", "true"); // 如果是一行一个对象，可设为 false


        // 设置Doris表名
        String database = parameterTool.get("doris.database", "battery_ods");
        String tableName;
        
        // 如果构造时指定了表名，则使用指定的表名
        if (targetTable != null) {
            tableName = targetTable;
        } else {
            tableName = parameterTool.get("doris.table", "error_data");
        }
        String table = database + "." + tableName;
        
        LOGGER.info("配置DorisSink: 表={}, 连接={}", table, parameterTool.get("doris.conn"));
        
        // 创建并返回DorisSink
        return DorisSink.sink(
            new DorisExecutionOptions.Builder()
              .setBatchIntervalMs((long)parameterTool.getInt("doris.batchIntervalMs", 2000))
              .setBatchSize(parameterTool.getInt("doris.batchSize", 1000))
              .setEnableDelete(false)
              .setMaxRetries(parameterTool.getInt("doris.maxRetries", Integer.MAX_VALUE))
              .setMaxBatchBytes(parameterTool.getLong("doris.maxBatchBytes", 1024 * 1024 * 1024L))
              .setStreamLoadProp(props)
              .build(),
            new DorisOptions.Builder()
              .setFenodes(parameterTool.get("doris.conn"))
              .setUsername(parameterTool.get("doris.user"))
              .setPassword(parameterTool.get("doris.passwd"))
              .setTableIdentifier(table)
              .build()
        );
    }
} 