package org.battery.quality.rule.adapter;

import org.battery.quality.rule.Rule;

/**
 * 规则适配器接口
 * 使用适配器模式处理不同类型规则的兼容性
 */
public interface RuleAdapter<T> {
    
    /**
     * 将目标对象适配为规则接口
     * 
     * @param target 目标对象
     * @return 规则实例
     */
    Rule adapt(T target);
    
    /**
     * 判断是否可以适配目标对象
     * 
     * @param target 目标对象
     * @return 是否可以适配
     */
    boolean canAdapt(T target);
} 