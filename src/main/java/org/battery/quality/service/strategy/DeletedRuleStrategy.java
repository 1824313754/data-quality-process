package org.battery.quality.service.strategy;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.battery.quality.service.RuleUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除规则策略
 */
public class DeletedRuleStrategy implements RuleChangeStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletedRuleStrategy.class);
    
    @Override
    public void handle(RuleEngine ruleEngine, RuleInfo ruleInfo, String ruleId, 
                      RuleService ruleService, RuleUpdateResult result) {
        try {
            LOGGER.info("开始处理删除规则: {}", ruleId);
            
            // 从引擎中移除规则
            ruleEngine.removeRule(ruleId);
            
            // 从本地快照中删除
            ruleService.removeFromLocalSnapshot(ruleId);
            
            result.deletedCount++;
            LOGGER.info("删除规则成功: {}", ruleId);
            
        } catch (Exception e) {
            LOGGER.error("删除规则失败: {}", ruleId, e);
            result.errorCount++;
        }
    }
}
