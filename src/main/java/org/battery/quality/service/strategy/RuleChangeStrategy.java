package org.battery.quality.service.strategy;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.battery.quality.service.RuleUpdateResult;

/**
 * 规则变更策略接口
 */
public interface RuleChangeStrategy {
    
    /**
     * 处理规则变更
     * 
     * @param ruleEngine 规则引擎
     * @param ruleInfo 规则信息（删除操作时可能为null）
     * @param ruleId 规则ID
     * @param ruleService 规则服务
     * @param result 更新结果
     */
    void handle(RuleEngine ruleEngine, RuleInfo ruleInfo, String ruleId, 
                RuleService ruleService, RuleUpdateResult result);
}
