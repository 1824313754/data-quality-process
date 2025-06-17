package org.battery.quality.source.impl;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.battery.model.Gb32960Data;
import org.battery.quality.source.DataSourceFactory;
import org.battery.quality.source.serialization.Gb32960DataDeserializationSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class KafkaDataSourceFactory implements DataSourceFactory {
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    @Value("${kafka.topic}")
    private String topic;
    
    @Value("${kafka.group.id}")
    private String groupId;
    
    @Autowired
    private DeserializationSchema<Gb32960Data> gb32960DataDeserializationSchema;

    @Override
    public DataStream<Gb32960Data> createSource(StreamExecutionEnvironment env) {
        KafkaSource<Gb32960Data> source = KafkaSource.<Gb32960Data>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(gb32960DataDeserializationSchema)
                .build();

        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
    }
} 