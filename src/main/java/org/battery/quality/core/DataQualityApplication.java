package org.battery.quality.core;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.model.BatteryData;
import org.battery.quality.model.ProcessedData;
import org.battery.quality.processor.RuleProcessor;
import org.battery.quality.sink.SinkManager;
import org.battery.quality.source.SourceManager;
import org.battery.quality.transformer.DataTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 电池数据质量分析系统主应用
 * 重构后采用分层架构，职责更加清晰
 */
public class DataQualityApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataQualityApplication.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("启动电池数据质量分析系统...");

        // 1. 加载配置
        AppConfig appConfig = ConfigManager.getInstance().getConfig();
        ParameterTool parameterTool = ParameterTool.fromMap(appConfig.toMap());
        
        // 2. 创建执行环境
        StreamExecutionEnvironment env = createExecutionEnvironment(appConfig);
        
        // 3. 创建数据源
        DataStream<BatteryData> sourceStream = SourceManager.createSource(env, appConfig);
        
        // 4. 处理数据
        DataStream<ProcessedData> processedStream = processData(sourceStream);
        
        // 5. 转换数据为输出格式
        DataStream<String> outputStream = DataTransformer.transformToJson(processedStream);
        
        // 6. 输出结果到Sink
        SinkManager.addSinks(outputStream, parameterTool);
        
        // 7. 执行任务
        env.execute("Battery Data Quality Analysis");
    }
    
    /**
     * 创建并配置Flink执行环境
     */
    private static StreamExecutionEnvironment createExecutionEnvironment(AppConfig config) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度
        env.setParallelism(config.getProcess().getParallelism());
        
        // 配置检查点
        env.enableCheckpointing(config.getProcess().getCheckpointInterval());
        
        return env;
    }
    
    /**
     * 处理数据流程
     */
    private static DataStream<ProcessedData> processData(DataStream<BatteryData> sourceStream) {
        return sourceStream
                .keyBy(data -> data.getVin())
                .process(new RuleProcessor());
    }
} 