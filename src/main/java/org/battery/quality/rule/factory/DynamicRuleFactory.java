package org.battery.quality.rule.factory;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.Rule;
import org.battery.quality.util.DynamicCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态规则工厂实现类
 * 通过动态编译源码创建规则实例
 */
public class DynamicRuleFactory implements RuleFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRuleFactory.class);
    
    @Override
    public Rule createRule(RuleInfo ruleInfo) {
        if (ruleInfo == null || ruleInfo.getSourceCode() == null || ruleInfo.getSourceCode().isEmpty()) {
            LOGGER.error("无法创建规则: 规则信息或源代码为空");
            return null;
        }
        
        try {
            // 动态编译规则类
            Class<?> ruleClass = DynamicCompiler.compile(ruleInfo.getName(), ruleInfo.getSourceCode());
            
            // 实例化规则
            return (Rule) ruleClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("动态编译规则失败: ID={}, 类名={}", 
                    ruleInfo.getId(), ruleInfo.getName(), e);
            return null;
        }
    }
    
    @Override
    public boolean canHandle(RuleInfo ruleInfo) {
        // 只要有源代码就可以处理
        return ruleInfo != null && ruleInfo.getSourceCode() != null && !ruleInfo.getSourceCode().isEmpty();
    }
} 