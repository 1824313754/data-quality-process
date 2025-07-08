package org.battery.quality.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.battery.quality.model.ProcessedData;
import org.battery.quality.model.QualityIssue;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理数据JSON映射器
 * 将处理后的数据转换为JSON格式
 */
@Slf4j
public class JsonMapper implements MapFunction<ProcessedData, String> {
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public String map(ProcessedData processedData) throws Exception {
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
            
            // 5. 转换为JSON字符串并返回
            String data = OBJECT_MAPPER.writeValueAsString(resultNode);
            return data;
        } catch (Exception e) {
            log.error("转换处理数据JSON失败", e);
            throw e;
        }
    }
} 