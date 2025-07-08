package org.battery.quality.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据质量问题模型类
 * 用于描述数据质量检测过程中发现的异常
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityIssue implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 问题编码，用于唯一标识问题类型
    private int code;
    
    // 问题值，描述问题的具体信息
    private String value;
    
    // 问题类型
    private String type;
    
    // 问题描述
    private String description;
    
    // 问题严重程度：1-低，2-中，3-高
    private int severity;
    
    /**
     * 创建简单的质量问题实例
     */
    public static QualityIssue simple(int code, String value) {
        return QualityIssue.builder()
                .code(code)
                .value(value)
                .severity(1)
                .build();
    }
} 