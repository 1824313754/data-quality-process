package org.battery.quality.rule;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 抽象规则基类
 * 实现了IRule接口的通用方法
 */
public abstract class AbstractRule implements IRule {
    
    @Override
    public String getType() {
        RuleDefinition annotation = this.getClass().getAnnotation(RuleDefinition.class);
        return annotation != null ? annotation.type() : this.getClass().getSimpleName();
    }
    
    @Override
    public int getCode() {
        RuleDefinition annotation = this.getClass().getAnnotation(RuleDefinition.class);
        return annotation != null ? annotation.code() : 0;
    }
    
    @Override
    public String getDescription() {
        RuleDefinition annotation = this.getClass().getAnnotation(RuleDefinition.class);
        return annotation != null ? annotation.description() : "";
    }
    
    @Override
    public RuleCategory getCategory() {
        RuleDefinition annotation = this.getClass().getAnnotation(RuleDefinition.class);
        return annotation != null ? annotation.category() : RuleCategory.VALIDITY;
    }
    
    @Override
    public int getPriority() {
        RuleDefinition annotation = this.getClass().getAnnotation(RuleDefinition.class);
        return annotation != null ? annotation.priority() : 0;
    }
    
    /**
     * 创建质量问题
     * @param data 原始数据
     * @param value 问题值
     * @return 质量问题对象
     */
    protected QualityIssue createIssue(BatteryData data, String value) {
        return QualityIssue.builder()
                .code(getCode())
                .type(getType())
                .description(getDescription())
                .value(value)
                .severity(getPriority())
                .build();
    }
    
    /**
     * 创建单个问题的列表
     * @param data 原始数据
     * @param value 问题值
     * @return 问题列表
     */
    protected List<QualityIssue> singleIssue(BatteryData data, String value) {
        List<QualityIssue> issues = new ArrayList<>(1);
        issues.add(createIssue(data, value));
        return issues;
    }
    
    /**
     * 返回空问题列表
     * @return 空列表
     */
    protected List<QualityIssue> noIssue() {
        return Collections.emptyList();
    }
} 