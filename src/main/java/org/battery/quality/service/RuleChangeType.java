package org.battery.quality.service;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.strategy.*;

/**
 * 规则变更类型枚举 - 策略模式实现
 * 每个枚举值关联一个具体的策略实现
 */
public enum RuleChangeType {

    /**
     * 新增规则
     */
    NEW("新增", new NewRuleStrategy()),

    /**
     * 修改规则
     */
    MODIFIED("修改", new ModifiedRuleStrategy()),

    /**
     * 删除规则
     */
    DELETED("删除", new DeletedRuleStrategy()),

    /**
     * 无变更
     */
    UNCHANGED("无变更", new UnchangedRuleStrategy());

    private final String description;
    private final RuleChangeStrategy strategy;

    RuleChangeType(String description, RuleChangeStrategy strategy) {
        this.description = description;
        this.strategy = strategy;
    }

    /**
     * 委托给具体策略处理
     *
     * @param ruleEngine 规则引擎
     * @param ruleInfo 规则信息（删除操作时可能为null）
     * @param ruleId 规则ID
     * @param ruleService 规则服务
     * @param result 更新结果
     */
    public void handle(RuleEngine ruleEngine, RuleInfo ruleInfo, String ruleId,
                      RuleService ruleService, RuleUpdateResult result) {
        strategy.handle(ruleEngine, ruleInfo, ruleId, ruleService, result);
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
