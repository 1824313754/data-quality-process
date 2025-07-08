package org.battery.quality.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.battery.quality.model.BatteryData;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Kafka电池数据反序列化模式
 * 用于将Kafka消息转换为BatteryData对象，并从Kafka元数据中获取时间戳
 */
@Slf4j
public class KafkaBatteryDataDeserializationSchema implements KafkaDeserializationSchema<BatteryData> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public BatteryData deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
        try {
            byte[] message = record.value();
            
            // 解析JSON字符串为BatteryData对象
            BatteryData batteryData = OBJECT_MAPPER.readValue(message, BatteryData.class);
            
            // 从Kafka元数据中获取时间戳
            long timestamp = record.timestamp();
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp), 
                    ZoneId.systemDefault());
            batteryData.setTime(dateTime.format(DATE_FORMATTER));
            
            // 解析JSON以获取ctime所需的字段
            JsonNode jsonNode = OBJECT_MAPPER.readTree(message);
            
            // 设置ctime字段，从JSON中获取时间相关字段并格式化
            int hours = jsonNode.path("hours").asInt();
            int seconds = jsonNode.path("seconds").asInt();
            int month = jsonNode.path("month").asInt();
            int year = 2000 + jsonNode.path("year").asInt(); // 25 -> 2025
            int minutes = jsonNode.path("minutes").asInt();
            int day = jsonNode.path("day").asInt();
            
            LocalDateTime ctimeDateTime = LocalDateTime.of(year, month, day, hours, minutes, seconds);
            batteryData.setCtime(ctimeDateTime.format(DATE_FORMATTER));
            
            // 处理温度数据（校正-40℃偏移）
            if (jsonNode.has("probeTemperatures") && jsonNode.path("probeTemperatures").isArray()) {
                List<Integer> temperatures = StreamSupport.stream(jsonNode.path("probeTemperatures").spliterator(), false)
                        .map(node -> node.isNull() ? null : node.asInt() - 40)
                        .collect(Collectors.toList());
                batteryData.setProbeTemperatures(temperatures);
            }
            
            // 处理电流数据（校正-10000偏移）
            if (jsonNode.has("totalCurrent") && !jsonNode.path("totalCurrent").isNull()) {
                int current = jsonNode.path("totalCurrent").asInt();
                batteryData.setTotalCurrent(current - 10000);
            }
            
            return batteryData;
            
        } catch (Exception e) {
            log.error("解析电池数据失败: {}", new String(record.value()), e);
            return null;
        }
    }

    @Override
    public boolean isEndOfStream(BatteryData nextElement) {
        return false;
    }
    
    @Override
    public TypeInformation<BatteryData> getProducedType() {
        return TypeInformation.of(BatteryData.class);
    }
} 