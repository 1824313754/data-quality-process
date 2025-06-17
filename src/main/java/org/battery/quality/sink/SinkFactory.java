package org.battery.quality.sink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.battery.quality.model.Gb32960DataWithIssues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink工厂类
 * 用于创建和选择合适的Sink实现
 */
public class SinkFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkFactory.class);

    /**
     * 根据配置创建合适的Sink
     *
     * @param parameterTool 参数工具
     * @return 可用于将数据写入目标存储的SinkFunction
     */
    public static SinkFunction<Gb32960DataWithIssues> createSink(ParameterTool parameterTool) {
        String sinkType = parameterTool.get("sink.type", "doris");
        LOGGER.info("创建Sink: type={}", sinkType);
        
        switch (sinkType.toLowerCase()) {
            case "doris":
                return new DorisSink().getSinkFunction(parameterTool);
            case "print":
                return new PrintSink().getSinkFunction(parameterTool);
            default:
                LOGGER.warn("未知的Sink类型: {}, 使用默认的DorisSink", sinkType);
                return new DorisSink().getSinkFunction(parameterTool);
        }
    }
    
    /**
     * 多重Sink，将数据同时写入多个目标
     */
    private static class MultipleSink implements Sink {
        private final SinkFunction<Gb32960DataWithIssues>[] sinks;
        
        @SafeVarargs
        public MultipleSink(SinkFunction<Gb32960DataWithIssues>... sinks) {
            this.sinks = sinks;
        }
        
        @Override
        public SinkFunction<Gb32960DataWithIssues> getSinkFunction(ParameterTool parameterTool) {
            return new SinkFunction<Gb32960DataWithIssues>() {
                @Override
                public void invoke(Gb32960DataWithIssues value, Context context) throws Exception {
                    // 将数据发送到所有配置的Sink
                    for (SinkFunction<Gb32960DataWithIssues> sink : sinks) {
                        sink.invoke(value, context);
                    }
                }
            };
        }
    }
} 