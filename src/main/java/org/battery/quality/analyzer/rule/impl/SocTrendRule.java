package org.battery.quality.analyzer.rule.impl;

import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.rule.AnalysisRule;
import org.battery.quality.model.AnalysisResult;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SocTrendRule implements AnalysisRule {
    
    @Override
    public void analyze(List<Gb32960Data> dataList, AnalysisResult result) {
        if (dataList == null || dataList.size() < 2) {
            return;
        }

        for (int i = 1; i < dataList.size(); i++) {
            Gb32960Data current = dataList.get(i);
            Gb32960Data previous = dataList.get(i - 1);
            
            if (current.getSoc() != null && previous.getSoc() != null) {
                int socDiff = current.getSoc() - previous.getSoc();
                // 如果SOC在非充电状态下突然增加超过10%
                if (current.getChargeStatus() != null && current.getChargeStatus() == 0 && socDiff > 10) {
                    result.addIssue("SOC异常增加: " + previous.getSoc() + " -> " + current.getSoc());
                }
            }
        }
    }
} 