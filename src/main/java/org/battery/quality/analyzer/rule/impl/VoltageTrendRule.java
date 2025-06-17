package org.battery.quality.analyzer.rule.impl;

import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.rule.AnalysisRule;
import org.battery.quality.model.AnalysisResult;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class VoltageTrendRule implements AnalysisRule {
    
    @Override
    public void analyze(List<Gb32960Data> dataList, AnalysisResult result) {
        if (dataList == null || dataList.size() < 2) {
            return;
        }

        for (int i = 1; i < dataList.size(); i++) {
            Gb32960Data current = dataList.get(i);
            Gb32960Data previous = dataList.get(i - 1);
            
            if (current.getTotalVoltage() != null && previous.getTotalVoltage() != null) {
                int voltageDiff = current.getTotalVoltage() - previous.getTotalVoltage();
                // 如果电压在短时间内变化超过50V
                if (Math.abs(voltageDiff) > 50) {
                    result.addIssue("电压异常变化: " + previous.getTotalVoltage() + " -> " + current.getTotalVoltage());
                }
            }
        }
    }
} 