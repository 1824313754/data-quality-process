package org.battery.quality.rule;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则引擎
 * 负责管理和执行规则
 */
public class RuleEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleEngine.class);
    
    // 规则缓存（规则类型 -> 规则实例）
    private final Map<String, IRule> ruleCache = new ConcurrentHashMap<>();
    
    // 默认车厂ID
    private static final String DEFAULT_FACTORY_ID = "0";
    
    // 车厂规则映射（车厂ID -> 规则类型列表）
    private final Map<String, List<String>> factoryRuleMapping = new ConcurrentHashMap<>();
    
    /**
     * 注册规则
     * 
     * @param rule 规则实例
     * @param factories 适用的车厂ID列表，为空则适用于所有车厂
     */
    public void registerRule(IRule rule, List<String> factories) {
        String ruleType = rule.getType();
        
        // 保存规则实例
        ruleCache.put(ruleType, rule);
        
        // 处理车厂规则映射
        if (factories == null || factories.isEmpty()) {
            // 如果没有指定车厂，添加到默认车厂
            addRuleToFactory(DEFAULT_FACTORY_ID, ruleType);
        } else {
            // 添加到指定车厂
            for (String factory : factories) {
                addRuleToFactory(factory, ruleType);
            }
        }
        
        LOGGER.info("注册规则: {}, 适用车厂: {}", ruleType, 
                factories == null ? "默认" : String.join(",", factories));
    }
    
    /**
     * 添加规则到车厂
     */
    private void addRuleToFactory(String factoryId, String ruleType) {
        factoryRuleMapping.computeIfAbsent(factoryId, k -> new ArrayList<>())
                .add(ruleType);
    }
    
    /**
     * 检查数据
     * 
     * @param data 电池数据
     * @param previousData 上一条数据（可能为null）
     * @param factoryId 车厂ID
     * @return 质量问题列表
     */
    public List<QualityIssue> checkData(BatteryData data, BatteryData previousData, String factoryId) {
        List<QualityIssue> allIssues = new ArrayList<>();
        
        // 如果factoryId为空，使用默认车厂ID
        if (factoryId == null || factoryId.isEmpty()) {
            factoryId = DEFAULT_FACTORY_ID;
        }
        
        // 获取适用于此车厂的规则
        List<String> ruleTypes = getRuleTypesForFactory(factoryId);
        
        // 执行每条规则
        for (String ruleType : ruleTypes) {
            IRule rule = ruleCache.get(ruleType);
            if (rule == null) {
                continue;
            }
            
            try {
                List<QualityIssue> issues;
                
                if (rule instanceof IStateRule && previousData != null) {
                    // 执行有状态规则
                    issues = ((IStateRule) rule).checkState(data, previousData);
                } else {
                    // 执行普通规则
                    issues = rule.check(data);
                }
                
                // 收集问题
                if (issues != null && !issues.isEmpty()) {
                    allIssues.addAll(issues);
                }
            } catch (Exception e) {
                LOGGER.error("规则执行异常: {}", ruleType, e);
            }
        }
        
        return allIssues;
    }
    
    /**
     * 获取适用于指定车厂的规则类型列表
     */
    private List<String> getRuleTypesForFactory(String factoryId) {
        List<String> result = new ArrayList<>();
        
        // 添加默认规则
        List<String> defaultRules = factoryRuleMapping.get(DEFAULT_FACTORY_ID);
        if (defaultRules != null) {
            result.addAll(defaultRules);
        }
        
        // 如果不是默认车厂，添加车厂特定规则
        if (!DEFAULT_FACTORY_ID.equals(factoryId)) {
            List<String> factoryRules = factoryRuleMapping.get(factoryId);
            if (factoryRules != null) {
                result.addAll(factoryRules);
            }
        }
        
        return result;
    }
    
    /**
     * 移除指定规则
     *
     * @param ruleType 规则类型
     */
    public void removeRule(String ruleType) {
        // 从规则缓存中移除
        IRule removedRule = ruleCache.remove(ruleType);

        if (removedRule != null) {
            // 从所有车厂映射中移除该规则
            factoryRuleMapping.values().forEach(ruleList -> ruleList.remove(ruleType));
            LOGGER.info("移除规则: {}", ruleType);
        } else {
            LOGGER.warn("尝试移除不存在的规则: {}", ruleType);
        }
    }

    /**
     * 检查规则是否存在
     *
     * @param ruleType 规则类型
     * @return 是否存在
     */
    public boolean hasRule(String ruleType) {
        return ruleCache.containsKey(ruleType);
    }

    /**
     * 获取规则实例
     *
     * @param ruleType 规则类型
     * @return 规则实例，不存在返回null
     */
    public IRule getRule(String ruleType) {
        return ruleCache.get(ruleType);
    }

    /**
     * 清除所有规则
     */
    public void clearRules() {
        ruleCache.clear();
        factoryRuleMapping.clear();
        LOGGER.info("清除所有规则");
    }

    /**
     * 获取已注册规则数量
     */
    public int getRuleCount() {
        return ruleCache.size();
    }

    /**
     * 获取所有规则类型
     */
    public Set<String> getAllRuleTypes() {
        return new HashSet<>(ruleCache.keySet());
    }
}