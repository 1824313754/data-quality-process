package org.battery.quality.rule;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;

import java.util.List;

/**
 * 数据质量规则接口
 * 定义规则的核心行为
 */
public interface IRule {
    /**
     * 检测单条数据
     * @param data 电池数据
     * @return 质量问题列表，如果没有问题则返回空列表
     */
    List<QualityIssue> check(BatteryData data);
    
    /**
     * 获取规则类型
     * @return 规则类型
     */
    String getType();
    
    /**
     * 获取规则编码
     * @return 规则编码
     */
    int getCode();
    
    /**
     * 获取规则描述
     * @return 规则描述
     */
    String getDescription();
    
    /**
     * 获取规则分类
     * @return 规则分类
     */
    RuleCategory getCategory();
    
    /**
     * 获取规则优先级
     * @return 规则优先级
     */
    int getPriority();
} 