package org.battery.quality.rule.strategy;

import org.battery.quality.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 策略选择器
 * 负责根据规则类型选择合适的执行策略
 */
public class StrategySelector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StrategySelector.class);
    
    /**
     * 使用线程安全的列表存储策略
     */
    private final List<RuleExecutionStrategy> strategies = new CopyOnWriteArrayList<>();
    
    /**
     * 单例实例
     */
    private static final StrategySelector INSTANCE = new StrategySelector();
    
    /**
     * 私有构造函数，注册默认策略
     */
    private StrategySelector() {
        // 注册默认策略
        registerStrategy(new StateRuleStrategy());
        registerStrategy(new StandardRuleStrategy());
    }
    
    /**
     * 获取单例实例
     * 
     * @return 策略选择器实例
     */
    public static StrategySelector getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册新的执行策略
     * 
     * @param strategy 执行策略
     */
    public void registerStrategy(RuleExecutionStrategy strategy) {
        if (strategy != null && !strategies.contains(strategy)) {
            // 优先级较高的策略插入到前面
            strategies.add(0, strategy);
            LOGGER.info("注册规则执行策略: {}", strategy.getClass().getName());
        }
    }
    
    /**
     * 移除执行策略
     * 
     * @param strategy 执行策略
     */
    public void unregisterStrategy(RuleExecutionStrategy strategy) {
        if (strategy != null) {
            strategies.remove(strategy);
            LOGGER.info("移除规则执行策略: {}", strategy.getClass().getName());
        }
    }
    
    /**
     * 选择合适的策略执行规则
     * 
     * @param rule 规则实例
     * @return 执行策略，如果没有找到合适的策略则返回null
     */
    public RuleExecutionStrategy selectStrategy(Rule rule) {
        if (rule == null) {
            return null;
        }
        
        for (RuleExecutionStrategy strategy : strategies) {
            if (strategy.supports(rule)) {
                return strategy;
            }
        }
        
        LOGGER.warn("没有找到适用于规则 {} 的执行策略", rule.getType());
        return null;
    }
    
    /**
     * 获取所有注册的策略
     * 
     * @return 策略列表的副本
     */
    public List<RuleExecutionStrategy> getStrategies() {
        return new ArrayList<>(strategies);
    }
} 