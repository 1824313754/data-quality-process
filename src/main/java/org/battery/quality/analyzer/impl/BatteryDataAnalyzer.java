package org.battery.quality.analyzer.impl;

import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.DataAnalyzer;
import org.battery.quality.analyzer.rule.AnalysisRule;
import org.battery.quality.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class BatteryDataAnalyzer implements DataAnalyzer {
    
    @Autowired
    private List<AnalysisRule> analysisRules;

    @Override
    public AnalysisResult analyzeWindow(List<Gb32960Data> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return null;
        }

        AnalysisResult result = new AnalysisResult();
        result.setVin(dataList.get(0).getVin());
        result.setWindowAnalysis(dataList.size() > 1);

        // 依次执行所有分析规则
        for (AnalysisRule rule : analysisRules) {
            rule.analyze(dataList, result);
        }

        return result;
    }
} 