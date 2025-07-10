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
 * 标准规则执行策略
 * 负责执行普通规则的检查逻辑
 */
public class StandardRuleStrategy implements RuleExecutionStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardRuleStrategy.class);
    
    @Override
    public List<Issue> execute(Rule rule, Gb32960Data data, Gb32960Data previousData) {
        try {
            return rule.check(data);
        } catch (Exception e) {
            LOGGER.error("执行规则 {} 时发生异常", rule.getType(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean supports(Rule rule) {
        // 支持所有非StateRule的规则
        return !(rule instanceof StateRule);
    }
} 