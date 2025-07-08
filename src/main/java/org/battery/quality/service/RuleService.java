package org.battery.quality.service;

import org.battery.quality.config.DatabaseManager;
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

/**
 * 规则服务
 * 处理规则的加载、编译和注册
 */
public class RuleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleService.class);
    
    // 规则DAO
    private final RuleDao ruleDao;
    
    /**
     * 构造函数
     */
    public RuleService() {
        this.ruleDao = new RuleDao();
    }
    
    /**
     * 加载规则并注册到规则引擎
     * 
     * @param ruleEngine 规则引擎
     * @return 加载的规则数量
     */
    public int loadRules(RuleEngine ruleEngine) {
        int count = 0;
        
        try {
            // 从数据库加载规则信息
            Map<String, RuleInfo> ruleInfoMap = ruleDao.loadAllRules();
            
            // 遍历规则信息
            for (RuleInfo ruleInfo : ruleInfoMap.values()) {
                try {
                    // 创建规则实例
                    IRule rule = createRule(ruleInfo);
                    if (rule == null) {
                        continue;
                    }
                    
                    // 解析适用的车厂ID列表
                    List<String> factories = parseFactories(ruleInfo.getEnabledFactories());
                    
                    // 注册规则到引擎
                    ruleEngine.registerRule(rule, factories);
                    
                    count++;
                } catch (Exception e) {
                    LOGGER.error("创建规则失败: {}", ruleInfo.getId(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("加载规则失败", e);
        }
        
        return count;
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