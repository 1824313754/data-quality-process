package org.battery.quality.service.strategy;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.IRule;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.battery.quality.service.RuleUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 新增规则策略
 */
public class NewRuleStrategy implements RuleChangeStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NewRuleStrategy.class);
    
    @Override
    public void handle(RuleEngine ruleEngine, RuleInfo ruleInfo, String ruleId, 
                      RuleService ruleService, RuleUpdateResult result) {
        try {
            LOGGER.info("开始处理新增规则: {}", ruleInfo.getId());
            
            // 编译规则
            IRule rule = ruleService.createRule(ruleInfo);
            if (rule == null) {
                LOGGER.error("编译规则失败: {}", ruleInfo.getId());
                result.errorCount++;
                return;
            }
            
            // 解析车厂列表
            List<String> factories = ruleService.parseFactories(ruleInfo.getEnabledFactories());
            
            // 注册到引擎
            ruleEngine.registerRule(rule, factories);
            
            // 更新本地快照
            ruleService.updateLocalSnapshot(ruleInfo.getId(), ruleInfo);
            
            result.addedCount++;
            LOGGER.info("新增规则成功: {}", ruleInfo.getId());
            
        } catch (Exception e) {
            LOGGER.error("新增规则失败: {}", ruleInfo.getId(), e);
            result.errorCount++;
        }
    }
}
