package org.battery.quality.analyzer.rule;

import org.battery.model.Gb32960Data;
import org.battery.quality.model.AnalysisResult;

import java.io.Serializable;
import java.util.List;

public interface AnalysisRule extends Serializable {
    /**
     * 分析数据
     * @param dataList 数据列表
     * @param result 分析结果
     */
    void analyze(List<Gb32960Data> dataList, AnalysisResult result);
} 