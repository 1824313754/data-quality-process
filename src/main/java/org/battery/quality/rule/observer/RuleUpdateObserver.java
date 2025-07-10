package org.battery.quality.rule.observer;

import org.battery.quality.model.RuleInfo;

import java.util.Map;

/**
 * 规则更新观察者接口
 * 使用观察者模式处理规则更新事件
 */
public interface RuleUpdateObserver {
    
    /**
     * 当规则更新时调用
     * 
     * @param changedRules 变更的规则信息映射，key为规则ID，value为规则信息(null表示规则被删除)
     */
    void onRuleUpdate(Map<String, RuleInfo> changedRules);
} 