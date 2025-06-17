package org.battery.quality.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.battery.quality.model.Gb32960DataWithIssues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 打印Sink实现
 * 将处理后的数据输出到控制台，支持标准输出或文件输出
 */
public class PrintSink implements Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintSink.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public SinkFunction<Gb32960DataWithIssues> getSinkFunction(ParameterTool parameterTool) {
        // 从参数中获取输出标识符（默认为质量检查结果）
        String identifier = parameterTool.get("print.identifier", "质量检查结果");
        
        // 从参数中获取是否打印详细信息
        boolean verbose = parameterTool.getBoolean("print.verbose", false);
        
        LOGGER.info("配置PrintSink: identifier={}, verbose={}", identifier, verbose);
        
        // 创建并返回打印Sink
        return new SinkFunction<Gb32960DataWithIssues>() {
            @Override
            public void invoke(Gb32960DataWithIssues value, Context context) {
                try {
                    if (verbose) {
                        // 详细模式：打印完整数据和问题
                        System.out.println(String.format("[%s] VIN: %s, 时间: %s, 问题数: %d", 
                                identifier,
                                value.getData().getVin(),
                                value.getData().getCtime(),
                                value.getIssues() != null ? value.getIssues().size() : 0));
                        
                        if (value.getIssues() != null && !value.getIssues().isEmpty()) {
                            System.out.println("检测到的问题:");
                            value.getIssues().forEach(issue -> 
                                System.out.println(String.format("  - [%s] %s: %s", 
                                        issue.getType(), 
                                        issue.getCode(), 
                                        issue.getDescription())));
                        }
                        System.out.println("---------------------------------");
                    } else {
                        // 简洁模式：仅打印基本信息
                        String jsonOutput = OBJECT_MAPPER.writeValueAsString(value);
                        System.out.println(String.format("[%s] %s", identifier, jsonOutput));
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.error("序列化数据失败", e);
                    System.out.println(String.format("[%s] 序列化失败: %s", identifier, e.getMessage()));
                }
            }
        };
    }
} 