package org.battery.quality.rule.chain;

import org.battery.quality.model.RuleType;
import org.battery.quality.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 规则链构建器
 * 使用构建者模式构建规则链
 */
public class RuleChainBuilder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleChainBuilder.class);
    
    /**
     * 规则映射，按规则类型分组
     */
    private final Map<RuleType, List<Rule>> rulesByType = new EnumMap<>(RuleType.class);
    
    /**
     * 默认规则链
     */
    private RuleChain defaultChain;
    
    /**
     * 类型规则链映射
     */
    private final Map<RuleType, RuleChain> typeChains = new EnumMap<>(RuleType.class);
    
    /**
     * 私有构造函数
     */
    private RuleChainBuilder() {
        defaultChain = new DefaultRuleChain();
        
        // 初始化所有规则类型的列表
        for (RuleType type : RuleType.values()) {
            rulesByType.put(type, new ArrayList<>());
            typeChains.put(type, new DefaultRuleChain());
        }
    }
    
    /**
     * 创建新的构建器实例
     * 
     * @return 构建器实例
     */
    public static RuleChainBuilder create() {
        return new RuleChainBuilder();
    }
    
    /**
     * 添加规则到构建器
     * 
     * @param rule 规则实例
     * @return 构建器实例
     */
    public RuleChainBuilder addRule(Rule rule) {
        if (rule != null) {
            RuleType category = rule.getCategory();
            rulesByType.get(category).add(rule);
            LOGGER.debug("添加规则到构建器: {}, 类型: {}", rule.getType(), category);
        }
        return this;
    }
    
    /**
     * 添加规则集合到构建器
     * 
     * @param rules 规则集合
     * @return 构建器实例
     */
    public RuleChainBuilder addRules(Collection<Rule> rules) {
        if (rules != null) {
            for (Rule rule : rules) {
                addRule(rule);
            }
        }
        return this;
    }
    
    /**
     * 构建规则链
     * 默认情况下，按照规则类型构建多个链，然后将它们连接起来
     * 
     * @return 构建好的规则链
     */
    public RuleChain build() {
        // 按照优先级顺序排列规则类型，影响规则执行顺序
        RuleType[] orderedTypes = {
            RuleType.COMPLETENESS,  // 完整性检查优先
            RuleType.VALIDITY,      // 其次是有效性检查
            RuleType.CONSISTENCY,   // 然后是一致性检查
            RuleType.TIMELINESS,    // 接着是时效性检查
            RuleType.LOGICAL        // 最后是逻辑检查
        };
        
        RuleChain firstChain = null;
        RuleChain previousChain = null;
        
        // 创建每种类型的规则链
        for (RuleType type : orderedTypes) {
            List<Rule> typeRules = rulesByType.get(type);
            if (typeRules != null && !typeRules.isEmpty()) {
                RuleChain chain = typeChains.get(type);
                
                // 将该类型的所有规则添加到对应的链中
                for (Rule rule : typeRules) {
                    chain.addRule(rule);
                }
                
                // 如果这是第一个链，记录下来
                if (firstChain == null) {
                    firstChain = chain;
                }
                
                // 连接链
                if (previousChain != null) {
                    previousChain.setNext(chain);
                }
                
                previousChain = chain;
            }
        }
        
        // 如果没有创建任何链，返回默认空链
        if (firstChain == null) {
            LOGGER.warn("没有规则被添加到链中，返回空链");
            return defaultChain;
        }
        
        return firstChain;
    }
    
    /**
     * 创建单一类型的规则链
     * 
     * @param type 规则类型
     * @return 包含指定类型规则的链
     */
    public RuleChain buildForType(RuleType type) {
        if (type == null) {
            return defaultChain;
        }
        
        RuleChain chain = new DefaultRuleChain();
        List<Rule> rules = rulesByType.get(type);
        
        if (rules != null) {
            for (Rule rule : rules) {
                chain.addRule(rule);
            }
        }
        
        return chain;
    }
    
    /**
     * 自定义规则链构建逻辑
     * 
     * @param customChain 自定义规则链
     * @return 构建器实例
     */
    public RuleChainBuilder setCustomChain(RuleChain customChain) {
        if (customChain != null) {
            this.defaultChain = customChain;
        }
        return this;
    }
    
    /**
     * 获取所有已添加的规则
     * 
     * @return 所有规则的列表
     */
    public List<Rule> getAllRules() {
        List<Rule> allRules = new ArrayList<>();
        for (List<Rule> typeRules : rulesByType.values()) {
            allRules.addAll(typeRules);
        }
        return allRules;
    }
} 