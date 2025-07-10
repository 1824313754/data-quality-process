package org.battery.quality.service.strategy;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.battery.quality.service.RuleUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 无变更规则策略
 */
public class UnchangedRuleStrategy implements RuleChangeStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UnchangedRuleStrategy.class);
    
    @Override
    public void handle(RuleEngine ruleEngine, RuleInfo ruleInfo, String ruleId, 
                      RuleService ruleService, RuleUpdateResult result) {
        // 无变更，不需要任何操作
        LOGGER.debug("规则无变更，跳过处理: {}", ruleInfo != null ? ruleInfo.getId() : ruleId);
    }
}
