package org.battery.quality.job;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.DataAnalyzer;
import org.battery.quality.model.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class DataQualityWindowFunction extends ProcessWindowFunction<Gb32960Data, String, String, TimeWindow> {
    
    private final DataAnalyzer dataAnalyzer;

    public DataQualityWindowFunction(DataAnalyzer dataAnalyzer) {
        this.dataAnalyzer = dataAnalyzer;
    }

    @Override
    public void process(String vin, Context context, Iterable<Gb32960Data> elements, Collector<String> out) throws Exception {
        List<Gb32960Data> dataList = new ArrayList<>();
        for (Gb32960Data data : elements) {
            dataList.add(data);
        }

        // 进行数据分析
        AnalysisResult result = dataAnalyzer.analyzeWindow(dataList);

        // 输出结果
        if (result != null && result.isHasIssue()) {
            StringBuilder output = new StringBuilder();
            output.append("VIN: ").append(vin).append("\n");
            output.append("Window: ").append(context.window().getStart()).append(" to ").append(context.window().getEnd()).append("\n");
            output.append("Analysis Results:\n");
            output.append("- ").append(result.getIssues()).append("\n");
            out.collect(output.toString());
        }
    }
} 