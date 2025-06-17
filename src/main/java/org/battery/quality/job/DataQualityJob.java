package org.battery.quality.job;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.DataAnalyzer;
import org.battery.quality.source.DataSourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataQualityJob {
    
    @Autowired
    private DataSourceFactory dataSourceFactory;
    
    @Autowired
    private DataAnalyzer dataAnalyzer;

    public void execute() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 创建数据源
        DataStream<Gb32960Data> dataStream = dataSourceFactory.createSource(env);

        // 按VIN分组并进行窗口处理
        dataStream
                .keyBy(Gb32960Data::getVin)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
                .process(new DataQualityWindowFunction(dataAnalyzer))
                .print();

        // 启动作业
        env.execute("Data Quality Analysis Job");
    }
} 