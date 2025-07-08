package org.battery.quality.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.battery.quality.model.ProcessedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据转换器
 * 负责将处理后的数据转换为适合输出的格式
 */
public class DataTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTransformer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 将处理后的数据转换为JSON字符串
     *
     * @param dataStream 处理后的数据流
     * @return JSON字符串数据流
     */
    public static DataStream<String> transformToJson(DataStream<ProcessedData> dataStream) {
        return dataStream.process(new ProcessFunction<ProcessedData, String>() {
            @Override
            public void processElement(ProcessedData data, Context ctx, Collector<String> out) throws Exception {
                try {
                    // 将对象转换为JSON字符串
                    String json = OBJECT_MAPPER.writeValueAsString(data);
                    out.collect(json);
                } catch (Exception e) {
                    LOGGER.error("转换JSON失败", e);
                }
            }
        }).name("JSON-Transformer");
    }
} 