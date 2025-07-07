package org.battery.quality.rule.observer;

import org.battery.quality.model.RuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 规则更新管理器
 * 实现规则更新主题接口，管理观察者并发送规则更新事件
 */
public class RuleUpdateManager implements RuleUpdateSubject {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleUpdateManager.class);
    
    /**
     * 使用线程安全的集合存储观察者
     */
    private final Set<RuleUpdateObserver> observers = new CopyOnWriteArraySet<>();
    
    /**
     * 变更的规则信息
     */
    private Map<String, RuleInfo> changedRules = new ConcurrentHashMap<>();
    
    /**
     * 单例实例
     */
    private static final RuleUpdateManager INSTANCE = new RuleUpdateManager();
    
    /**
     * 私有构造函数
     */
    private RuleUpdateManager() {}
    
    /**
     * 获取单例实例
     * 
     * @return 规则更新管理器实例
     */
    public static RuleUpdateManager getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void registerObserver(RuleUpdateObserver observer) {
        if (observer != null) {
            observers.add(observer);
            LOGGER.debug("注册规则更新观察者: {}", observer.getClass().getName());
        }
    }
    
    @Override
    public void removeObserver(RuleUpdateObserver observer) {
        if (observer != null) {
            observers.remove(observer);
            LOGGER.debug("移除规则更新观察者: {}", observer.getClass().getName());
        }
    }
    
    @Override
    public void notifyObservers() {
        if (changedRules.isEmpty()) {
            LOGGER.debug("没有规则变更，跳过通知");
            return;
        }
        
        LOGGER.info("通知所有观察者规则更新，变更规则数量: {}", changedRules.size());
        
        // 创建变更规则的副本，避免在通知过程中被修改
        Map<String, RuleInfo> changedRulesCopy = new ConcurrentHashMap<>(changedRules);
        
        // 通知所有观察者
        for (RuleUpdateObserver observer : observers) {
            try {
                observer.onRuleUpdate(changedRulesCopy);
            } catch (Exception e) {
                LOGGER.error("通知观察者时发生异常: {}", observer.getClass().getName(), e);
            }
        }
        
        // 清空变更记录
        changedRules.clear();
    }
    
    /**
     * 更新规则信息
     * 
     * @param ruleId 规则ID
     * @param ruleInfo 规则信息，为null表示规则被删除
     */
    public void updateRule(String ruleId, RuleInfo ruleInfo) {
        if (ruleId != null && !ruleId.isEmpty()) {
            changedRules.put(ruleId, ruleInfo);
            LOGGER.debug("更新规则信息: {}", ruleId);
        }
    }
    
    /**
     * 批量更新规则信息
     * 
     * @param rules 规则信息映射
     */
    public void updateRules(Map<String, RuleInfo> rules) {
        if (rules != null && !rules.isEmpty()) {
            changedRules.putAll(rules);
            LOGGER.debug("批量更新规则信息，数量: {}", rules.size());
        }
    }
    
    /**
     * 获取当前变更的规则信息
     * 
     * @return 变更规则信息的副本
     */
    public Map<String, RuleInfo> getChangedRules() {
        return new ConcurrentHashMap<>(changedRules);
    }
} 