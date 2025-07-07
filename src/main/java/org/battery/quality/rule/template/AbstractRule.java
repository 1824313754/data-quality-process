package org.battery.quality.rule.template;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.Rule;
import org.battery.quality.rule.annotation.QualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 抽象规则类
 * 使用模板方法模式实现通用规则逻辑
 */
public abstract class AbstractRule implements Rule {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRule.class);
    
    @Override
    public final List<Issue> check(Gb32960Data data) {
        if (data == null) {
            LOGGER.warn("规则 {} 检查时收到null数据", getType());
            return Collections.emptyList();
        }
        
        try {
            // 前置条件检查
            if (!preCheck(data)) {
                return Collections.emptyList();
            }
            
            // 执行具体的检查逻辑
            return doCheck(data);
        } catch (Exception e) {
            LOGGER.error("规则 {} 执行异常", getType(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 前置条件检查
     * 子类可以重写此方法进行数据有效性检查
     * 
     * @param data 车辆数据
     * @return 是否继续检查，返回false则跳过后续检查
     */
    protected boolean preCheck(Gb32960Data data) {
        return true;
    }
    
    /**
     * 执行具体的检查逻辑
     * 子类必须实现此方法提供具体的规则检查逻辑
     * 
     * @param data 车辆数据
     * @return 检查结果，包含发现的问题
     */
    protected abstract List<Issue> doCheck(Gb32960Data data);
    
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
     * 
     * @param data 原始数据
     * @param value 异常值
     * @return 异常对象
     */
    protected Issue createIssue(Gb32960Data data, String value) {
        return Issue.builder()
                .code(getCode())
                .value(value)
                .build();
    }
    
    /**
     * 创建一个包含单个异常的列表
     * 
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
     * 
     * @return 空列表
     */
    protected List<Issue> noIssue() {
        return Collections.emptyList();
    }
} 