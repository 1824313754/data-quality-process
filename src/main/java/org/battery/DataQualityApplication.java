package org.battery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Gb32960DataWithIssues;
import org.battery.quality.processor.BroadcastRuleProcessor;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.sink.SinkFactory;
import org.battery.quality.source.RuleBroadcastSource;
import org.battery.quality.source.Gb32960DeserializationSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

/**
 * 电池数据质量分析主应用
 */
public class DataQualityApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataQualityApplication.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("启动电池数据质量分析系统...");

        // 加载应用配置
        AppConfig appConfig = AppConfigLoader.load();
        
        // 创建参数工具
        final ParameterTool parameterTool = ParameterTool.fromMap(appConfig.toMap());

        // 创建执行环境
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(appConfig.getProcess().getParallelism());
      
//        // 配置检查点
//        env.enableCheckpointing(appConfig.getProcess().getCheckpointInterval());

        // 创建规则广播状态描述符
        MapStateDescriptor<String, Map<String, RuleInfo>> ruleStateDescriptor = 
                new MapStateDescriptor<>("RulesBroadcastState", 
                        Types.STRING, 
                        Types.MAP(Types.STRING, Types.GENERIC(RuleInfo.class)));
                        
        // 创建规则广播源
        DataStream<Map<String, RuleInfo>> ruleStream = env
                .addSource(new RuleBroadcastSource(appConfig.getMysql().getCacheRefreshInterval()))
                .name("Rule-Broadcast-Source");
                
        // 创建广播流
        BroadcastStream<Map<String, RuleInfo>> ruleBroadcastStream = 
                ruleStream.broadcast(ruleStateDescriptor);
                
        LOGGER.info("规则广播流创建完成");

        // 创建Kafka数据源
        KafkaSource<Gb32960Data> source = KafkaSource.<Gb32960Data>builder()
                .setBootstrapServers(appConfig.getKafka().getBootstrapServers())
                .setTopics(appConfig.getKafka().getTopic())
                .setGroupId(appConfig.getKafka().getGroupId())
                .setStartingOffsets(OffsetsInitializer.latest())
                .setDeserializer(new Gb32960DeserializationSchema())
                .build();

        // 创建数据流
        DataStream<Gb32960Data> dataStream = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source");

        // 连接数据流和规则广播流
        SingleOutputStreamOperator<Gb32960DataWithIssues> resultsStream = dataStream
                .keyBy(data -> data.getVin())
                .connect(ruleBroadcastStream)
                .process(new BroadcastRuleProcessor(ruleStateDescriptor))
                .name("Rule-Processor");
        
        // 获取侧输出流 - 数据统计
        DataStream<String> dataStatsStream = resultsStream
                .getSideOutput(BroadcastRuleProcessor.DATA_STATS_TAG);
                
        // 将数据统计写入Doris
        dataStatsStream
                .addSink(SinkFactory.createSink(parameterTool, "normal_data_stats"))
                .name("Data-Stats-Sink");
                
        // 处理主输出流 - 异常数据
        DataStream<String> jsonStream = resultsStream.map(new MapFunction<Gb32960DataWithIssues, String>() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public String map(Gb32960DataWithIssues value) throws Exception {
                // 把整个对象转成JsonNode树
                JsonNode root = mapper.valueToTree(value);

                // 取出 data 节点（应该是一个ObjectNode）
                ObjectNode dataNode = (ObjectNode) root.get("data");
                //取出time,转为date
                String time = dataNode.get("time").asText();
                String date = time.substring(0,10);
                dataNode.put("day_of_year", date);
                
                if (dataNode == null || dataNode.isNull()) {
                    throw new RuntimeException("Missing `data` field in Gb32960DataWithIssues");
                }
                
                // 取出 issues 节点
                JsonNode issuesNode = root.get("issues");

                // 把 issues 节点放到 data 节点里，字段名叫 "issues"
                dataNode.set("issues", issuesNode);

                // 返回合并后的 data 节点的 JSON 字符串
                return mapper.writeValueAsString(dataNode);
            }
        });

        // 使用SinkFactory创建的Sink将结果写入存储
        jsonStream.addSink(SinkFactory.createSink(parameterTool, "error_data"))
                .name("Data-Quality-Sink");

        // 执行任务
        env.execute("Battery Data Quality Analysis");
    }
} 