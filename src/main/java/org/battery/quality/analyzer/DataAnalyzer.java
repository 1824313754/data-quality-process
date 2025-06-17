package org.battery.quality.analyzer;

import org.battery.model.Gb32960Data;
import org.battery.quality.model.AnalysisResult;

import java.io.Serializable;
import java.util.List;

public interface DataAnalyzer extends Serializable {
    /**
     * 分析数据窗口
     * @param dataList 窗口内的数据列表
     * @return 分析结果
     */
    AnalysisResult analyzeWindow(List<Gb32960Data> dataList);
} 