package org.battery.quality.rule.chain;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.Rule;
import org.battery.quality.rule.strategy.RuleExecutionStrategy;
import org.battery.quality.rule.strategy.StrategySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认规则链实现
 */
public class DefaultRuleChain implements RuleChain {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRuleChain.class);
    
    /**
     * 链中的规则，使用Map保证查找和删除的高效性
     */
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
    
    /**
     * 下一个规则链
     */
    private RuleChain next;
    
    /**
     * 策略选择器
     */
    private final StrategySelector strategySelector;
    
    /**
     * 构造函数
     */
    public DefaultRuleChain() {
        this.strategySelector = StrategySelector.getInstance();
    }
    
    /**
     * 构造函数，指定策略选择器
     * 
     * @param strategySelector 策略选择器
     */
    public DefaultRuleChain(StrategySelector strategySelector) {
        this.strategySelector = strategySelector != null ? strategySelector : StrategySelector.getInstance();
    }
    
    @Override
    public RuleChain addRule(Rule rule) {
        if (rule != null) {
            rules.put(rule.getType(), rule);
            LOGGER.debug("规则链添加规则: {}", rule.getType());
        }
        return this;
    }
    
    @Override
    public RuleChain removeRule(String ruleId) {
        if (ruleId != null && !ruleId.isEmpty()) {
            rules.remove(ruleId);
            LOGGER.debug("规则链移除规则: {}", ruleId);
        }
        return this;
    }
    
    @Override
    public RuleChain setNext(RuleChain next) {
        this.next = next;
        return this;
    }
    
    @Override
    public List<Issue> execute(Gb32960Data data, Gb32960Data previousData) {
        if (data == null) {
            LOGGER.warn("规则链执行时收到null数据");
            return Collections.emptyList();
        }
        
        List<Issue> allIssues = new ArrayList<>();
        
        // 执行当前链中的所有规则
        for (Rule rule : rules.values()) {
            RuleExecutionStrategy strategy = strategySelector.selectStrategy(rule);
            if (strategy != null) {
                List<Issue> issues = strategy.execute(rule, data, previousData);
                if (issues != null && !issues.isEmpty()) {
                    allIssues.addAll(issues);
                }
            } else {
                LOGGER.warn("规则 {} 没有找到合适的执行策略", rule.getType());
            }
        }
        
        // 执行下一个链
        if (next != null) {
            List<Issue> nextIssues = next.execute(data, previousData);
            if (nextIssues != null && !nextIssues.isEmpty()) {
                allIssues.addAll(nextIssues);
            }
        }
        
        return allIssues;
    }
    
    @Override
    public int size() {
        return rules.size();
    }
    
    @Override
    public boolean isEmpty() {
        return rules.isEmpty();
    }
} 