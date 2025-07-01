package org.battery.quality.sink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.battery.quality.model.Gb32960DataWithIssues;

/**
 * 数据输出接口
 * 定义将处理后的数据写入到目标存储的方法
 */
public interface Sink {

    /**
     * 获取可用于添加到流中的SinkFunction
     *
     * @param parameterTool 参数工具
     * @return 可用于将数据写入目标存储的SinkFunction
     */
    SinkFunction<String> getSinkFunction(ParameterTool parameterTool);
} 