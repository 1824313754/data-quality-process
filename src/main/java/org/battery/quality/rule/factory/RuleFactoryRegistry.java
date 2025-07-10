package org.battery.quality.rule.factory;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 规则工厂注册表
 * 管理所有规则工厂，并选择合适的工厂创建规则
 */
public class RuleFactoryRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleFactoryRegistry.class);
    
    /**
     * 采用CopyOnWriteArrayList，保证线程安全，适合读多写少的场景
     */
    private final List<RuleFactory> factories = new CopyOnWriteArrayList<>();
    
    /**
     * 单例实例
     */
    private static final RuleFactoryRegistry INSTANCE = new RuleFactoryRegistry();
    
    /**
     * 私有构造函数，注册默认工厂
     */
    private RuleFactoryRegistry() {
        // 注册默认工厂
        registerFactory(new DynamicRuleFactory());
    }
    
    /**
     * 获取单例实例
     * 
     * @return 规则工厂注册表实例
     */
    public static RuleFactoryRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册新的规则工厂
     * 
     * @param factory 规则工厂
     */
    public void registerFactory(RuleFactory factory) {
        if (factory != null && !factories.contains(factory)) {
            factories.add(factory);
            LOGGER.info("注册规则工厂: {}", factory.getClass().getName());
        }
    }
    
    /**
     * 移除规则工厂
     * 
     * @param factory 规则工厂
     */
    public void unregisterFactory(RuleFactory factory) {
        if (factory != null) {
            factories.remove(factory);
            LOGGER.info("移除规则工厂: {}", factory.getClass().getName());
        }
    }
    
    /**
     * 创建规则实例
     * 
     * @param ruleInfo 规则信息
     * @return 规则实例，如果没有合适的工厂则返回null
     */
    public Rule createRule(RuleInfo ruleInfo) {
        if (ruleInfo == null) {
            return null;
        }
        
        for (RuleFactory factory : factories) {
            if (factory.canHandle(ruleInfo)) {
                Rule rule = factory.createRule(ruleInfo);
                if (rule != null) {
                    LOGGER.debug("使用工厂 {} 创建规则: {}", factory.getClass().getSimpleName(), ruleInfo.getId());
                    return rule;
                }
            }
        }
        
        LOGGER.warn("没有找到合适的工厂创建规则: {}", ruleInfo.getId());
        return null;
    }
    
    /**
     * 获取所有注册的工厂
     * 
     * @return 工厂列表的副本
     */
    public List<RuleFactory> getFactories() {
        return new ArrayList<>(factories);
    }
} 