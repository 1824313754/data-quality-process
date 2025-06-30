package org.battery.quality.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.battery.quality.model.Gb32960Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 国标32960数据反序列化Schema
 * 实现KafkaRecordDeserializationSchema接口，以便能够从Kafka元数据中获取时间戳
 */
public class Gb32960DeserializationSchema implements KafkaRecordDeserializationSchema<Gb32960Data> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Gb32960DeserializationSchema.class);
    private transient ObjectMapper objectMapper;
    
    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<Gb32960Data> out) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        
        try {
            byte[] message = record.value();
            if (message == null) {
                return;
            }
            
            Gb32960Data data = objectMapper.readValue(message, Gb32960Data.class);
            if (data != null) {
                // 设置time字段为Kafka消息的时间戳
                data.setTime(record.timestamp());
                out.collect(data);
            }
        } catch (Exception e) {
            LOGGER.error("反序列化数据失败: {}", new String(record.value()), e);
        }
    }

    @Override
    public TypeInformation<Gb32960Data> getProducedType() {
        return TypeInformation.of(Gb32960Data.class);
    }
} 