package org.battery.quality.service;

import org.battery.quality.dao.RuleDao;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.IRule;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.util.DynamicCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
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

            // 检测所有规则的变更状态
            Map<String, RuleChangeType> ruleChanges = detectAllRuleChanges(latestRules);

            // 使用策略模式处理每个规则的变更
            for (Map.Entry<String, RuleChangeType> entry : ruleChanges.entrySet()) {
                String ruleId = entry.getKey();
                RuleChangeType changeType = entry.getValue();
                RuleInfo ruleInfo = latestRules.get(ruleId);

                // 跳过无变更的规则
                if (changeType == RuleChangeType.UNCHANGED) {
                    continue;
                }

                // 委托给对应的策略处理
                changeType.handle(ruleEngine, ruleInfo, ruleId, this, result);
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
     * 检测所有规则的变更状态
     */
    private Map<String, RuleChangeType> detectAllRuleChanges(Map<String, RuleInfo> latestRules) {
        Map<String, RuleChangeType> changes = new HashMap<>();

        // 检测删除的规则
        for (String localRuleId : localRuleSnapshot.keySet()) {
            if (!latestRules.containsKey(localRuleId)) {
                changes.put(localRuleId, RuleChangeType.DELETED);
            }
        }

        // 检测新增和修改的规则
        for (RuleInfo latestRule : latestRules.values()) {
            RuleInfo localRule = localRuleSnapshot.get(latestRule.getId());

            if (localRule == null) {
                // 新增的规则
                changes.put(latestRule.getId(), RuleChangeType.NEW);
            } else if (isRuleModified(localRule, latestRule)) {
                // 修改的规则
                changes.put(latestRule.getId(), RuleChangeType.MODIFIED);
            } else {
                // 无变更的规则
                changes.put(latestRule.getId(), RuleChangeType.UNCHANGED);
            }
        }

        return changes;
    }

    /**
     * 更新本地快照（供策略调用）
     */
    public void updateLocalSnapshot(String ruleId, RuleInfo ruleInfo) {
        localRuleSnapshot.put(ruleId, ruleInfo);
    }

    /**
     * 从本地快照中删除（供策略调用）
     */
    public void removeFromLocalSnapshot(String ruleId) {
        localRuleSnapshot.remove(ruleId);
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
     * 创建规则实例（供策略调用）
     *
     * @param ruleInfo 规则信息
     * @return 规则实例
     */
    public IRule createRule(RuleInfo ruleInfo) {
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
     * 解析车厂ID列表（供策略调用）
     *
     * @param enabledFactories 逗号分隔的车厂ID字符串
     * @return 车厂ID列表
     */
    public List<String> parseFactories(String enabledFactories) {
        if (enabledFactories == null || enabledFactories.trim().isEmpty()) {
            return null;
        }
        
        return Arrays.asList(enabledFactories.split(","));
    }
} 