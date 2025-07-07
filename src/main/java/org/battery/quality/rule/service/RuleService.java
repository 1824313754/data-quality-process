package org.battery.quality.rule.service;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.Rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 规则服务接口
 * 定义规则相关的操作
 */
public interface RuleService {
    
    /**
     * 上传所有规则到数据库
     * 
     * @return 上传成功的规则数量
     */
    int uploadAllRules();
    
    /**
     * 获取所有可用规则
     * 
     * @return 规则ID到规则信息的映射
     */
    Map<String, RuleInfo> getAllRules();
    
    /**
     * 获取规则信息
     * 
     * @param ruleId 规则ID
     * @return 规则信息的Optional包装
     */
    Optional<RuleInfo> getRuleById(String ruleId);
    
    /**
     * 获取特定车厂可用的规则
     * 
     * @param factoryId 车厂ID
     * @return 规则列表
     */
    List<Rule> getFactoryRules(String factoryId);
    
    /**
     * 获取规则变更
     * 
     * @param lastUpdateTime 上次更新时间
     * @return 变更的规则信息
     */
    Map<String, RuleInfo> getRuleChanges(long lastUpdateTime);
    
    /**
     * 启用规则
     * 
     * @param ruleId 规则ID
     * @param factoryId 车厂ID
     * @return 是否成功
     */
    boolean enableRule(String ruleId, String factoryId);
    
    /**
     * 禁用规则
     * 
     * @param ruleId 规则ID
     * @param factoryId 车厂ID
     * @return 是否成功
     */
    boolean disableRule(String ruleId, String factoryId);
    
    /**
     * 创建规则实例
     * 
     * @param ruleInfo 规则信息
     * @return 规则实例的Optional包装
     */
    Optional<Rule> createRule(RuleInfo ruleInfo);
    
    /**
     * 清除规则缓存
     */
    void clearCache();
} 