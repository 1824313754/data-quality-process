package org.battery.quality.analyzer.rule.impl;

import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.rule.AnalysisRule;
import org.battery.quality.model.AnalysisResult;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TemperatureTrendRule implements AnalysisRule {
    
    @Override
    public void analyze(List<Gb32960Data> dataList, AnalysisResult result) {
        if (dataList == null || dataList.size() < 2) {
            return;
        }

        for (int i = 1; i < dataList.size(); i++) {
            Gb32960Data current = dataList.get(i);
            Gb32960Data previous = dataList.get(i - 1);
            
            if (current.getMaxTemperature() != null && previous.getMaxTemperature() != null) {
                int tempDiff = current.getMaxTemperature() - previous.getMaxTemperature();
                // 如果温度在短时间内变化超过10度
                if (Math.abs(tempDiff) > 10) {
                    result.addIssue("温度异常变化: " + previous.getMaxTemperature() + " -> " + current.getMaxTemperature());
                }
            }
        }
    }
} 