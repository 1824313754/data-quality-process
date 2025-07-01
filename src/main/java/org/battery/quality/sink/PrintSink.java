package org.battery.quality.sink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 打印Sink实现
 * 将处理后的数据输出到控制台，支持标准输出或文件输出
 */
public class PrintSink implements Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintSink.class);

    @Override
    public SinkFunction<String> getSinkFunction(ParameterTool parameterTool) {
        // 从参数中获取输出标识符（默认为质量检查结果）
        String identifier = parameterTool.get("print.identifier", "质量检查结果");
        
        LOGGER.info("配置PrintSink: identifier={}", identifier);
        
        // 创建并返回打印Sink
        return new SinkFunction<String>() {
            @Override
            public void invoke(String value, Context context) {
                // 直接打印JSON字符串
                System.out.println(String.format("[%s] %s", identifier, value));
            }
        };
    }
} 