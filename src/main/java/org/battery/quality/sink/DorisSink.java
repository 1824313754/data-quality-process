package org.battery.quality.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.battery.quality.model.Gb32960DataWithIssues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Doris数据库Sink实现
 * 提供将处理后的数据写入Doris数据库的SinkFunction
 */
public class DorisSink implements Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(DorisSink.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public SinkFunction<Gb32960DataWithIssues> getSinkFunction(ParameterTool parameterTool) {
        // 准备StreamLoad参数
        Properties pro = new Properties();
        pro.setProperty("format", "json");
        pro.setProperty("read_json_by_line", "true");
        pro.setProperty("strip_outer_array", "true");
        
        // 设置Doris表名
        String database = parameterTool.get("doris.database", "battery_data");
        String tableName = parameterTool.get("doris.table", "gb32960_data_with_issues");
        String table = database + "." + tableName;
        
        LOGGER.info("配置DorisSink: 表={}, 连接={}", table, parameterTool.get("doris.conn"));
        
        // 创建并返回DorisSink
        return org.apache.doris.flink.cfg.DorisSink.sink(
            new DorisExecutionOptions.Builder()
              .setBatchIntervalMs((long)parameterTool.getInt("doris.batchIntervalMs", 2000))
              .setBatchSize(parameterTool.getInt("doris.batchSize", 1000))
              .setEnableDelete(false)
              .setMaxRetries(parameterTool.getInt("doris.maxRetries", Integer.MAX_VALUE))
              .setMaxBatchBytes(parameterTool.getLong("doris.maxBatchBytes", 1024 * 1024 * 1024L))
              .setStreamLoadProp(pro)
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