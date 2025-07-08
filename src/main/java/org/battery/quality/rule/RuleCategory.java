package org.battery.quality.rule;

/**
 * 规则分类枚举
 * 定义数据质量规则的分类
 */
public enum RuleCategory {
    /**
     * 有效性规则
     * 检查数据是否有效、合法
     */
    VALIDITY,
    
    /**
     * 完整性规则
     * 检查数据是否完整
     */
    COMPLETENESS,
    
    /**
     * 一致性规则
     * 检查数据是否一致
     */
    CONSISTENCY,
    
    /**
     * 及时性规则
     * 检查数据是否及时
     */
    TIMELINESS,
    
    /**
     * 准确性规则
     * 检查数据是否准确
     */
    ACCURACY,
    
    /**
     * 唯一性规则
     * 检查数据是否唯一
     */
    UNIQUENESS,
    
    /**
     * 自定义规则
     * 用户自定义的规则
     */
    CUSTOM
} 