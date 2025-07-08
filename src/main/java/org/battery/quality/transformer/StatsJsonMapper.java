package org.battery.quality.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.battery.quality.model.DataStats;

/**
 * 统计数据JSON映射器
 * 将统计数据转换为JSON格式
 */
@Slf4j
public class StatsJsonMapper implements MapFunction<DataStats, String> {
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public String map(DataStats stats) throws Exception {
        try {
            // 直接使用ObjectMapper将对象转换为JSON
            String data = OBJECT_MAPPER.writeValueAsString(stats);
            return data;
        } catch (Exception e) {
            log.error("转换统计数据JSON失败", e);
            throw e;
        }
    }
} 