package org.battery.quality.analyzer.rule.impl;

import org.battery.model.Gb32960Data;
import org.battery.quality.analyzer.rule.AnalysisRule;
import org.battery.quality.model.AnalysisResult;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class BasicDataRule implements AnalysisRule {
    
    @Override
    public void analyze(List<Gb32960Data> dataList, AnalysisResult result) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        // 只对单条数据进行基础检查
        if (dataList.size() == 1) {
            Gb32960Data data = dataList.get(0);
            
            // 检查VIN是否为空
            if (data.getVin() == null || data.getVin().trim().isEmpty()) {
                result.addIssue("VIN不能为空");
            }

            // 检查经纬度范围
            if (data.getLongitude() != null && (data.getLongitude() < -180 || data.getLongitude() > 180)) {
                result.addIssue("经度超出有效范围");
            }
            if (data.getLatitude() != null && (data.getLatitude() < -90 || data.getLatitude() > 90)) {
                result.addIssue("纬度超出有效范围");
            }

            // 检查SOC范围
            if (data.getSoc() != null && (data.getSoc() < 0 || data.getSoc() > 100)) {
                result.addIssue("SOC超出有效范围(0-100)");
            }

            // 检查电压范围
            if (data.getTotalVoltage() != null && (data.getTotalVoltage() < 0 || data.getTotalVoltage() > 1000)) {
                result.addIssue("总电压超出有效范围");
            }
        }
    }
} 