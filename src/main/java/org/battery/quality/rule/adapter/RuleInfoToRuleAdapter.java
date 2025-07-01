package org.battery.quality.rule.adapter;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.Rule;
import org.battery.quality.rule.factory.RuleFactoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 规则信息到规则的适配器
 * 将RuleInfo对象适配为Rule接口
 */
public class RuleInfoToRuleAdapter implements RuleAdapter<RuleInfo> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleInfoToRuleAdapter.class);
    
    /**
     * 规则工厂注册表
     */
    private final RuleFactoryRegistry factoryRegistry;
    
    /**
     * 构造函数
     */
    public RuleInfoToRuleAdapter() {
        this.factoryRegistry = RuleFactoryRegistry.getInstance();
    }
    
    /**
     * 构造函数，指定工厂注册表
     * 
     * @param factoryRegistry 规则工厂注册表
     */
    public RuleInfoToRuleAdapter(RuleFactoryRegistry factoryRegistry) {
        this.factoryRegistry = factoryRegistry != null ? 
                factoryRegistry : RuleFactoryRegistry.getInstance();
    }
    
    @Override
    public Rule adapt(RuleInfo ruleInfo) {
        if (!canAdapt(ruleInfo)) {
            LOGGER.error("无法适配无效的规则信息");
            return null;
        }
        
        return factoryRegistry.createRule(ruleInfo);
    }
    
    @Override
    public boolean canAdapt(RuleInfo ruleInfo) {
        return ruleInfo != null && ruleInfo.getName() != null && !ruleInfo.getName().isEmpty();
    }
} 