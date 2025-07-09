package org.battery.quality.service;

import org.battery.quality.dao.RuleDao;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.IRule;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.util.DynamicCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则服务
 * 处理规则的动态加载、编译和注册
 *
 * 核心功能：
 * 1. 增量更新 - 检测规则变更并只更新变化的部分
 * 2. 三种变更场景：新增、修改、删除
 * 3. 基于更新时间的变更检测
 */
public class RuleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleService.class);

    // 规则DAO
    private final RuleDao ruleDao;

    // 本地规则快照：规则ID -> 规则信息（用于变更检测）
    private final Map<String, RuleInfo> localRuleSnapshot = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public RuleService() {
        this.ruleDao = new RuleDao();
    }
    
    /**
     * 增量更新规则到规则引擎
     *
     * @param ruleEngine 规则引擎
     * @return 更新统计信息
     */
    public RuleUpdateResult updateRules(RuleEngine ruleEngine) {
        RuleUpdateResult result = new RuleUpdateResult();

        try {
            // 从数据库加载最新规则信息
            Map<String, RuleInfo> latestRules = ruleDao.loadAllRules();

            // 检测变更
            RuleChanges changes = detectChanges(latestRules);

            // 处理删除的规则
            for (String deletedRuleId : changes.getDeletedRules()) {
                ruleEngine.removeRule(deletedRuleId);
                localRuleSnapshot.remove(deletedRuleId);
                result.deletedCount++;
                LOGGER.info("删除规则: {}", deletedRuleId);
            }

            // 处理新增和修改的规则
            for (RuleInfo ruleInfo : changes.getAddedOrModifiedRules()) {
                try {
                    // 如果是修改的规则，先删除旧版本
                    if (ruleEngine.hasRule(ruleInfo.getId())) {
                        ruleEngine.removeRule(ruleInfo.getId());
                        result.modifiedCount++;
                        LOGGER.info("修改规则: {}", ruleInfo.getId());
                    } else {
                        result.addedCount++;
                        LOGGER.info("新增规则: {}", ruleInfo.getId());
                    }

                    // 编译并注册新规则
                    IRule rule = createRule(ruleInfo);
                    if (rule != null) {
                        List<String> factories = parseFactories(ruleInfo.getEnabledFactories());
                        ruleEngine.registerRule(rule, factories);

                        // 更新本地快照
                        localRuleSnapshot.put(ruleInfo.getId(), ruleInfo);
                    }
                } catch (Exception e) {
                    LOGGER.error("处理规则失败: {}", ruleInfo.getId(), e);
                    result.errorCount++;
                }
            }

            LOGGER.info("规则更新完成 - 新增:{}, 修改:{}, 删除:{}, 错误:{}",
                    result.addedCount, result.modifiedCount, result.deletedCount, result.errorCount);

        } catch (Exception e) {
            LOGGER.error("更新规则失败", e);
            result.errorCount++;
        }

        return result;
    }

    /**
     * 检测规则变更
     */
    private RuleChanges detectChanges(Map<String, RuleInfo> latestRules) {
        RuleChanges changes = new RuleChanges();

        // 检测删除的规则
        for (String localRuleId : localRuleSnapshot.keySet()) {
            if (!latestRules.containsKey(localRuleId)) {
                changes.addDeletedRule(localRuleId);
            }
        }

        // 检测新增和修改的规则
        for (RuleInfo latestRule : latestRules.values()) {
            RuleInfo localRule = localRuleSnapshot.get(latestRule.getId());

            if (localRule == null) {
                // 新增的规则
                changes.addAddedOrModifiedRule(latestRule);
            } else if (isRuleModified(localRule, latestRule)) {
                // 修改的规则
                changes.addAddedOrModifiedRule(latestRule);
            }
        }

        return changes;
    }

    /**
     * 判断规则是否被修改
     */
    private boolean isRuleModified(RuleInfo localRule, RuleInfo latestRule) {
        // 比较更新时间
        if (localRule.getUpdateTime() == null || latestRule.getUpdateTime() == null) {
            return true; // 如果时间为空，认为需要更新
        }

        return !localRule.getUpdateTime().equals(latestRule.getUpdateTime());
    }
    
    /**
     * 创建规则实例
     * 
     * @param ruleInfo 规则信息
     * @return 规则实例
     */
    private IRule createRule(RuleInfo ruleInfo) {
        try {
            // 编译规则类
            Class<?> ruleClass = DynamicCompiler.compile(
                    ruleInfo.getName(),
                    ruleInfo.getSourceCode());
            
            if (ruleClass == null) {
                LOGGER.error("编译规则类失败: {}", ruleInfo.getId());
                return null;
            }
            
            // 创建规则实例
            Object instance = ruleClass.getDeclaredConstructor().newInstance();
            
            // 检查是否实现了IRule接口
            if (instance instanceof IRule) {
                return (IRule) instance;
            } else {
                LOGGER.error("规则类 {} 未实现IRule接口", ruleInfo.getName());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("创建规则实例失败: {}", ruleInfo.getId(), e);
            return null;
        }
    }
    
    /**
     * 解析车厂ID列表
     * 
     * @param enabledFactories 逗号分隔的车厂ID字符串
     * @return 车厂ID列表
     */
    private List<String> parseFactories(String enabledFactories) {
        if (enabledFactories == null || enabledFactories.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.asList(enabledFactories.split(","));
    }
} 