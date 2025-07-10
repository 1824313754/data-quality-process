package org.battery.quality.rule.chain;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.Rule;

import java.util.List;

/**
 * 规则执行链接口
 * 使用责任链模式处理规则执行流程
 */
public interface RuleChain {
    
    /**
     * 将规则添加到链中
     * 
     * @param rule 规则实例
     * @return 当前链，支持链式调用
     */
    RuleChain addRule(Rule rule);
    
    /**
     * 从链中移除规则
     * 
     * @param ruleId 规则ID
     * @return 当前链，支持链式调用
     */
    RuleChain removeRule(String ruleId);
    
    /**
     * 设置下一个规则链
     * 
     * @param next 下一个规则链
     * @return 当前链，支持链式调用
     */
    RuleChain setNext(RuleChain next);
    
    /**
     * 执行规则链
     * 
     * @param data 当前数据
     * @param previousData 前一条数据
     * @return 所有问题的集合
     */
    List<Issue> execute(Gb32960Data data, Gb32960Data previousData);
    
    /**
     * 获取链中规则的数量
     * 
     * @return 规则数量
     */
    int size();
    
    /**
     * 检查链是否为空
     * 
     * @return 如果链为空则返回true
     */
    boolean isEmpty();
} 