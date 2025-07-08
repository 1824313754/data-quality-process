package org.battery.quality.sink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink管理器
 * 负责创建和管理所有输出目标
 */
public class SinkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkManager.class);
    
    /**
     * 为数据流添加输出目标
     * 
     * @param dataStream 数据流
     * @param params 参数
     */
    public static void addSinks(DataStream<String> dataStream, ParameterTool params) {
        String sinkType = params.get("sink.type", "doris");
        LOGGER.info("使用输出类型: {}", sinkType);
        
        // 创建主Sink并添加到数据流
        SinkFunction<String> sink = SinkFactory.createSink(params);
        dataStream.addSink(sink).name("Quality-" + sinkType + "-Sink");
        
        // 如果同时需要控制台输出用于调试，添加额外的Print Sink
        if (params.getBoolean("debug", false)) {
            LOGGER.info("启用调试输出");
            SinkFunction<String> printSink = new PrintSink().getSinkFunction(params);
            dataStream.addSink(printSink).name("Debug-Print-Sink");
        }
    }
} 