package org.battery.quality.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 处理后的数据模型类
 * 包含原始数据和处理后发现的质量问题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 原始电池数据
    private BatteryData data;
    
    // 发现的质量问题列表
    private List<QualityIssue> issues;
    
    /**
     * 获取质量问题数量
     */
    public int getIssueCount() {
        return issues == null ? 0 : issues.size();
    }
    
    /**
     * 是否存在质量问题
     */
    public boolean hasIssues() {
        return issues != null && !issues.isEmpty();
    }
} 