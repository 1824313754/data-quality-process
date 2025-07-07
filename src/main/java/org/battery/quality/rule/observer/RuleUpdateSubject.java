package org.battery.quality.rule.observer;

/**
 * 规则更新主题接口
 * 负责管理观察者并发送规则更新事件
 */
public interface RuleUpdateSubject {
    
    /**
     * 注册观察者
     * 
     * @param observer 观察者
     */
    void registerObserver(RuleUpdateObserver observer);
    
    /**
     * 移除观察者
     * 
     * @param observer 观察者
     */
    void removeObserver(RuleUpdateObserver observer);
    
    /**
     * 通知所有观察者
     */
    void notifyObservers();
} 