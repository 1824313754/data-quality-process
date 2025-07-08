package org.battery.quality;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.model.BatteryData;
import org.battery.quality.model.DataStats;
import org.battery.quality.model.ProcessedData;
import org.battery.quality.processor.RuleProcessor;
import org.battery.quality.sink.FlinkDorisSink;
import org.battery.quality.transformer.JsonMapper;
import org.battery.quality.transformer.StatsJsonMapper;
import org.battery.quality.source.SourceManager;

/**
 * 电池数据质量分析系统主应用
 * 采用扁平化设计，减少层级嵌套，逻辑更加直观
 */
@Slf4j
public class DataQualityApplication {

    public static void main(String[] args) throws Exception {
        log.info("启动电池数据质量分析系统...");
        
        // 1. 初始化配置
        AppConfig appConfig = ConfigManager.getInstance().getConfig();
        ParameterTool parameterTool = ParameterTool.fromMap(appConfig.toMap());
        
        // 2. 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(appConfig.getProcess().getParallelism());
        env.enableCheckpointing(appConfig.getProcess().getCheckpointInterval());
        
        // 3. 创建数据源
        log.info("创建数据源...");
        DataStream<BatteryData> sourceStream = SourceManager.createSource(env, appConfig);
        
        // 4. 应用规则处理
        log.info("处理数据...");
        SingleOutputStreamOperator<ProcessedData> processedStream = sourceStream
                .keyBy(data -> data.getVin())
                .process(new RuleProcessor());
        
        // 5. 转换主数据流为输出格式
        DataStream<String> outputStream = processedStream
                .map(new JsonMapper())
                .name("主数据-JSON转换");
        
        // 6. 获取数据统计侧输出流
        DataStream<DataStats> statsStream = processedStream.getSideOutput(RuleProcessor.STATS_OUTPUT_TAG);
        log.info("已获取数据统计侧输出流");
        
        // 7. 转换数据统计流为JSON字符串
        DataStream<String> statsJsonStream = statsStream
                .map(new StatsJsonMapper())
                .name("统计数据-JSON转换");
        
        // 8. 配置主数据流输出
        log.info("配置主数据流输出...");
        String sinkType = parameterTool.get("sink.type", "doris");
        log.info("使用输出类型: {}", sinkType);
 
        FlinkDorisSink dorisSink = new FlinkDorisSink();
        outputStream.addSink(dorisSink.getSinkFunction(parameterTool)).name("Quality-Doris-Sink");
        log.info("已添加Doris输出");

        // 9. 配置统计数据流输出
        log.info("配置统计数据流输出...");
        FlinkDorisSink statsSink = new FlinkDorisSink()
                .setTableName("normal_data_stats");
        statsJsonStream.addSink(statsSink.getSinkFunction(parameterTool));
        log.info("数据统计流已添加到Doris Sink，表名: normal_data_stats");
        
        // 10. 执行任务
        env.execute("Battery Data Quality Analysis");
    }
} 