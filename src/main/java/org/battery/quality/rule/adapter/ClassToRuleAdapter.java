package org.battery.quality.rule.adapter;

import org.battery.quality.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类到规则的适配器
 * 将Class对象适配为Rule接口
 */
public class ClassToRuleAdapter implements RuleAdapter<Class<?>> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassToRuleAdapter.class);
    
    @Override
    public Rule adapt(Class<?> targetClass) {
        if (!canAdapt(targetClass)) {
            LOGGER.error("无法将类 {} 适配为规则", targetClass.getName());
            return null;
        }
        
        try {
            // 实例化规则
            return (Rule) targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("实例化规则类 {} 失败", targetClass.getName(), e);
            return null;
        }
    }
    
    @Override
    public boolean canAdapt(Class<?> targetClass) {
        if (targetClass == null) {
            return false;
        }
        
        // 检查是否实现了Rule接口
        return Rule.class.isAssignableFrom(targetClass);
    }
} 