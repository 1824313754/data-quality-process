package org.battery.quality.rule;

import org.battery.quality.model.Issue;
import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基础规则实现类
 */
public abstract class BaseRule implements Rule {
    
    @Override
    public String getType() {
        QualityRule annotation = this.getClass().getAnnotation(QualityRule.class);
        return annotation != null ? annotation.type() : this.getClass().getSimpleName();
    }
    
    @Override
    public int getCode() {
        QualityRule annotation = this.getClass().getAnnotation(QualityRule.class);
        return annotation != null ? annotation.code() : 0;
    }
    
    @Override
    public String getDescription() {
        QualityRule annotation = this.getClass().getAnnotation(QualityRule.class);
        return annotation != null ? annotation.description() : "";
    }
    
    @Override
    public RuleType getCategory() {
        QualityRule annotation = this.getClass().getAnnotation(QualityRule.class);
        return annotation != null ? annotation.category() : RuleType.VALIDITY;
    }
    
    @Override
    public int getPriority() {
        QualityRule annotation = this.getClass().getAnnotation(QualityRule.class);
        return annotation != null ? annotation.priority() : 0;
    }
    
    /**
     * 创建异常信息
     * @param data 原始数据
     * @param value 异常值
     * @return 异常对象
     */
    protected Issue createIssue(Gb32960Data data, String value) {
        return Issue.builder()
                .id(getType())
                .code(getCode())
                .vin(data.getVin())
                .description(getDescription())
                .value(value)
                .type(getCategory().name())
                .timestamp(data.getCtime())
                .build();
    }
    
    /**
     * 创建一个包含单个异常的列表
     * @param data 原始数据
     * @param value 异常值
     * @return 异常列表
     */
    protected List<Issue> singleIssue(Gb32960Data data, String value) {
        List<Issue> issues = new ArrayList<>(1);
        issues.add(createIssue(data, value));
        return issues;
    }
    
    /**
     * 如果没有异常，返回空列表
     * @return 空列表
     */
    protected List<Issue> noIssue() {
        return Collections.emptyList();
    }
} 