package org.battery.quality.rule.adapter;

import org.battery.quality.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 适配器注册表
 * 管理所有规则适配器，并选择合适的适配器转换对象
 */
@SuppressWarnings("unchecked")
public class AdapterRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterRegistry.class);
    
    /**
     * 适配器映射，按目标类型分组
     */
    private final Map<Class<?>, List<RuleAdapter<?>>> adaptersByType = new ConcurrentHashMap<>();
    
    /**
     * 单例实例
     */
    private static final AdapterRegistry INSTANCE = new AdapterRegistry();
    
    /**
     * 私有构造函数，注册默认适配器
     */
    private AdapterRegistry() {
        // 注册默认适配器
        registerAdapter(new ClassToRuleAdapter());
        registerAdapter(new RuleInfoToRuleAdapter());
    }
    
    /**
     * 获取单例实例
     * 
     * @return 适配器注册表实例
     */
    public static AdapterRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册适配器
     * 
     * @param adapter 适配器实例
     * @param <T> 适配目标类型
     */
    public <T> void registerAdapter(RuleAdapter<T> adapter) {
        if (adapter == null) {
            return;
        }
        
        // 获取适配器的泛型类型
        Class<?> targetType = findTargetType(adapter);
        if (targetType == null) {
            LOGGER.warn("无法确定适配器 {} 的目标类型", adapter.getClass().getName());
            return;
        }
        
        // 获取或创建适配器列表
        adaptersByType.computeIfAbsent(targetType, k -> new ArrayList<>())
                .add(adapter);
        
        LOGGER.info("注册适配器: {}, 目标类型: {}", adapter.getClass().getName(), targetType.getName());
    }
    
    /**
     * 查找适配器的目标类型
     * 
     * @param adapter 适配器实例
     * @return 目标类型Class对象
     */
    private Class<?> findTargetType(RuleAdapter<?> adapter) {
        // 这里简化实现，仅支持默认适配器
        if (adapter instanceof ClassToRuleAdapter) {
            return Class.class;
        } else if (adapter instanceof RuleInfoToRuleAdapter) {
            return org.battery.quality.model.RuleInfo.class;
        }
        
        // 对于自定义适配器，无法确定其泛型类型，需要手动指定
        return null;
    }
    
    /**
     * 适配对象为规则
     * 
     * @param target 目标对象
     * @param <T> 目标类型
     * @return 规则实例，如果无法适配则返回null
     */
    public <T> Rule adapt(T target) {
        if (target == null) {
            return null;
        }
        
        // 获取目标对象的类型
        Class<?> targetType = target.getClass();
        
        // 查找适配器
        List<RuleAdapter<?>> adapters = findAdapters(targetType);
        if (adapters.isEmpty()) {
            LOGGER.warn("没有找到适配器处理类型: {}", targetType.getName());
            return null;
        }
        
        // 尝试每个适配器
        for (RuleAdapter<?> adapter : adapters) {
            try {
                RuleAdapter<T> typedAdapter = (RuleAdapter<T>) adapter;
                if (typedAdapter.canAdapt(target)) {
                    Rule rule = typedAdapter.adapt(target);
                    if (rule != null) {
                        return rule;
                    }
                }
            } catch (ClassCastException e) {
                LOGGER.debug("适配器 {} 不适用于目标对象类型 {}", 
                        adapter.getClass().getName(), targetType.getName());
            }
        }
        
        LOGGER.warn("无法将对象适配为规则: {}", target);
        return null;
    }
    
    /**
     * 查找可以处理指定类型的所有适配器
     * 
     * @param targetType 目标类型
     * @return 适配器列表
     */
    private List<RuleAdapter<?>> findAdapters(Class<?> targetType) {
        List<RuleAdapter<?>> result = new ArrayList<>();
        
        // 尝试精确匹配
        List<RuleAdapter<?>> exactMatches = adaptersByType.get(targetType);
        if (exactMatches != null) {
            result.addAll(exactMatches);
        }
        
        // 尝试匹配父类和接口
        for (Map.Entry<Class<?>, List<RuleAdapter<?>>> entry : adaptersByType.entrySet()) {
            if (entry.getKey().isAssignableFrom(targetType) && !entry.getKey().equals(targetType)) {
                result.addAll(entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 获取所有注册的适配器
     * 
     * @return 适配器映射的副本
     */
    public Map<Class<?>, List<RuleAdapter<?>>> getAdapters() {
        Map<Class<?>, List<RuleAdapter<?>>> result = new HashMap<>();
        for (Map.Entry<Class<?>, List<RuleAdapter<?>>> entry : adaptersByType.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
} 