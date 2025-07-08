package org.battery.quality.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.battery.quality.model.ProcessedData;
import org.battery.quality.model.QualityIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据转换器
 * 负责将处理后的数据转换为适合输出的格式
 */
public class DataTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTransformer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 将处理后的数据转换为JSON字符串
     * 将原始数据作为主体，并将质量问题以{code:value}的形式添加
     *
     * @param dataStream 处理后的数据流
     * @return JSON字符串数据流
     */
    public static DataStream<String> transformToJson(DataStream<ProcessedData> dataStream) {
        return dataStream.process(new ProcessFunction<ProcessedData, String>() {
            @Override
            public void processElement(ProcessedData processedData, Context ctx, Collector<String> out) throws Exception {
                try {
                    // 1. 获取原始电池数据
                    Object batteryData = processedData.getData();
                    
                    // 2. 将原始数据转换为JSON节点
                    ObjectNode resultNode = OBJECT_MAPPER.valueToTree(batteryData);
                    
                    // 3. 提取质量问题的code和value
                    Map<String, String> issuesMap = new HashMap<>();
                    if (processedData.getIssues() != null) {
                        for (QualityIssue issue : processedData.getIssues()) {
                            issuesMap.put(String.valueOf(issue.getCode()), issue.getValue());
                        }
                    }
                    
                    // 4. 将质量问题添加到结果中
                    resultNode.set("issues", OBJECT_MAPPER.valueToTree(issuesMap));
                    
                    // 5. 转换为JSON字符串并输出
                    String json = OBJECT_MAPPER.writeValueAsString(resultNode);
                    out.collect(json);
                } catch (Exception e) {
                    LOGGER.error("转换JSON失败", e);
                }
            }
        }).name("JSON-Transformer");
    }
} 