package org.battery.quality.source;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.battery.quality.config.AppConfig;
import org.battery.quality.model.BatteryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 数据源管理器
 * 负责创建和配置数据源
 */
public class SourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceManager.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 创建数据源
     * 
     * @param env Flink执行环境
     * @param config 应用配置
     * @return 数据流
     */
    public static DataStream<BatteryData> createSource(StreamExecutionEnvironment env, AppConfig config) {
        LOGGER.info("创建数据源，类型：Kafka");
        
        // 创建Kafka消费者配置
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", config.getKafka().getBootstrapServers());
        kafkaProps.setProperty("group.id", config.getKafka().getGroupId());
        kafkaProps.setProperty("auto.offset.reset", config.getKafka().getAutoOffsetReset());
        
        // 创建消费者，使用支持获取Kafka元数据的反序列化模式
        FlinkKafkaConsumer<BatteryData> consumer = new FlinkKafkaConsumer<>(
            config.getKafka().getTopic(),
            new KafkaBatteryDataDeserializationSchema(),
            kafkaProps
        );
        
        // 设置消费起始位置
        consumer.setStartFromLatest();
        
        // 创建数据流
        DataStream<BatteryData> sourceStream = env
            .addSource(consumer)
            .name("Kafka-Battery-Data-Source")
            .uid("kafka-source");
        
        // 设置水印策略
        return sourceStream;
    }
} 