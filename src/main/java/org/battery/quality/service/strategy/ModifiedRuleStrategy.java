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
 * 修改规则策略
 */
public class ModifiedRuleStrategy implements RuleChangeStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifiedRuleStrategy.class);
    
    @Override
    public void handle(RuleEngine ruleEngine, RuleInfo ruleInfo, String ruleId, 
                      RuleService ruleService, RuleUpdateResult result) {
        try {
            LOGGER.info("开始处理修改规则: {}", ruleInfo.getId());
            
            // 1. 先移除旧版本
            ruleEngine.removeRule(ruleInfo.getId());
            LOGGER.debug("移除旧版本规则: {}", ruleInfo.getId());
            
            // 2. 编译新版本
            IRule rule = ruleService.createRule(ruleInfo);
            if (rule == null) {
                LOGGER.error("编译新版本规则失败: {}", ruleInfo.getId());
                result.errorCount++;
                return;
            }
            
            // 3. 解析车厂列表
            List<String> factories = ruleService.parseFactories(ruleInfo.getEnabledFactories());
            
            // 4. 注册新版本到引擎
            ruleEngine.registerRule(rule, factories);
            
            // 5. 更新本地快照
            ruleService.updateLocalSnapshot(ruleInfo.getId(), ruleInfo);
            
            result.modifiedCount++;
            LOGGER.info("修改规则成功: {}", ruleInfo.getId());
            
        } catch (Exception e) {
            LOGGER.error("修改规则失败: {}", ruleInfo.getId(), e);
            result.errorCount++;
        }
    }
}
