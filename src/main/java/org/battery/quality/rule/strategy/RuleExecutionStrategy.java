package org.battery.quality.rule.strategy;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.Rule;

import java.util.List;

/**
 * 规则执行策略接口
 * 使用策略模式实现不同的规则执行逻辑
 */
public interface RuleExecutionStrategy {
    
    /**
     * 执行规则检查
     * 
     * @param rule 规则实例
     * @param data 当前数据
     * @param previousData 前一条数据，可能为null
     * @return 质量问题列表
     */
    List<Issue> execute(Rule rule, Gb32960Data data, Gb32960Data previousData);
    
    /**
     * 判断此策略是否适用于特定规则
     * 
     * @param rule 规则实例
     * @return 是否适用
     */
    boolean supports(Rule rule);
} 