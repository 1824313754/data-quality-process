package org.battery.quality.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 包含数据和质量问题的包装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gb32960DataWithIssues {
    // 原始数据
    private Gb32960Data data;
    
    // 质量问题列表
    private List<Issue> issues;
} 