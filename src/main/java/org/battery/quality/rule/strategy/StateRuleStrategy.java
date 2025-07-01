package org.battery.quality.rule.strategy;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.Rule;
import org.battery.quality.rule.StateRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * 状态规则执行策略
 * 负责执行需要维护状态的规则的检查逻辑
 */
public class StateRuleStrategy implements RuleExecutionStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StateRuleStrategy.class);
    
    @Override
    public List<Issue> execute(Rule rule, Gb32960Data data, Gb32960Data previousData) {
        if (!(rule instanceof StateRule)) {
            LOGGER.warn("尝试使用状态规则策略执行非状态规则: {}", rule.getType());
            return Collections.emptyList();
        }
        
        try {
            StateRule stateRule = (StateRule) rule;
            return stateRule.checkState(data, previousData);
        } catch (Exception e) {
            LOGGER.error("执行状态规则 {} 时发生异常", rule.getType(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean supports(Rule rule) {
        return rule instanceof StateRule;
    }
} 