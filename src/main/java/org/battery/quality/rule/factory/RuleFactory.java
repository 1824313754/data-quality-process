package org.battery.quality.rule.factory;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.Rule;

/**
 * 规则工厂接口
 * 使用工厂模式创建规则实例
 */
public interface RuleFactory {
    
    /**
     * 创建规则实例
     * 
     * @param ruleInfo 规则信息
     * @return 规则实例
     */
    Rule createRule(RuleInfo ruleInfo);
    
    /**
     * 判断工厂是否能够处理特定类型的规则
     * 
     * @param ruleInfo 规则信息
     * @return 是否可以处理
     */
    boolean canHandle(RuleInfo ruleInfo);
} 