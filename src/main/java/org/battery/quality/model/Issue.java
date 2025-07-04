package org.battery.quality.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据质量异常模型类
 * 简化版本，只包含异常编码和异常值
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    // 异常编码
    private int code;
    
    // 异常值
    private String value;
} 