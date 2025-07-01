package org.battery.quality.rule;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.adapter.AdapterRegistry;
import org.battery.quality.rule.chain.RuleChain;
import org.battery.quality.rule.chain.RuleChainBuilder;
import org.battery.quality.rule.factory.RuleFactoryRegistry;
import org.battery.quality.rule.observer.RuleUpdateManager;
import org.battery.quality.rule.observer.RuleUpdateObserver;
import org.battery.quality.rule.service.DefaultRuleService;
import org.battery.quality.rule.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则管理器
 * 使用多种设计模式重构，提供规则管理的统一入口
 */
public class RuleManager implements RuleUpdateObserver {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleManager.class);
    
    /**
     * 车厂规则链缓存，key为车厂ID
     */
    private final Map<String, RuleChain> factoryRuleChains = new ConcurrentHashMap<>();
    
    /**
     * 规则服务
     */
    private final RuleService ruleService;
    
    /**
     * 规则工厂注册表
     */
    private final RuleFactoryRegistry factoryRegistry;
    
    /**
     * 适配器注册表
     */
    private final AdapterRegistry adapterRegistry;
    
    /**
     * 规则更新管理器
     */
    private final RuleUpdateManager updateManager;
    
    /**
     * 单例实例
     */
    private static final RuleManager INSTANCE = new RuleManager();
    
    /**
     * 私有构造函数
     */
    private RuleManager() {
        this.ruleService = new DefaultRuleService();
        this.factoryRegistry = RuleFactoryRegistry.getInstance();
        this.adapterRegistry = AdapterRegistry.getInstance();
        this.updateManager = RuleUpdateManager.getInstance();
        
        // 注册为观察者
        this.updateManager.registerObserver(this);
    }
    
    /**
     * 获取单例实例
     * 
     * @return 规则管理器实例
     */
    public static RuleManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 上传所有规则
     * 
     * @return 上传成功的规则数量
     */
    public int uploadAllRules() {
        return ruleService.uploadAllRules();
    }
    
    /**
     * 检查数据，应用所有适用的规则
     * 
     * @param data 当前数据
     * @param previousData 前一条数据，可能为null
     * @return 检查发现的问题列表
     */
    public List<Issue> checkData(Gb32960Data data, Gb32960Data previousData) {
        if (data == null) {
            LOGGER.warn("收到null数据，跳过检查");
            return Collections.emptyList();
        }
        
        // 获取车厂ID
        String factoryId = data.getVehicleFactory();
        
        // 获取或创建车厂规则链
        RuleChain ruleChain = getOrCreateRuleChain(factoryId);
        
        // 执行规则链
        return ruleChain.execute(data, previousData);
    }
    
    /**
     * 获取或创建车厂的规则链
     * 
     * @param factoryId 车厂ID
     * @return 规则链
     */
    private RuleChain getOrCreateRuleChain(String factoryId) {
        // 从缓存获取规则链
        RuleChain chain = factoryRuleChains.get(factoryId);
        
        // 如果缓存中没有，创建新的规则链
        if (chain == null) {
            chain = createRuleChain(factoryId);
            factoryRuleChains.put(factoryId, chain);
        }
        
        return chain;
    }
    
    /**
     * 创建车厂的规则链
     * 
     * @param factoryId 车厂ID
     * @return 规则链
     */
    private RuleChain createRuleChain(String factoryId) {
        // 获取车厂可用的规则
        List<Rule> rules = ruleService.getFactoryRules(factoryId);
        
        // 使用规则链构建器创建规则链
        return RuleChainBuilder.create()
                .addRules(rules)
                .build();
    }
    
    /**
     * 获取规则信息
     * 
     * @param ruleId 规则ID
     * @return 规则信息的Optional包装
     */
    public Optional<RuleInfo> getRuleById(String ruleId) {
        return ruleService.getRuleById(ruleId);
    }
    
    /**
     * 获取所有规则
     * 
     * @return 规则ID到规则信息的映射
     */
    public Map<String, RuleInfo> getAllRules() {
        return ruleService.getAllRules();
    }
    
    /**
     * 获取规则变更
     * 
     * @param lastUpdateTime 上次更新时间
     * @return 变更的规则信息
     */
    public Map<String, RuleInfo> getRuleChanges(long lastUpdateTime) {
        return ruleService.getRuleChanges(lastUpdateTime);
    }
    
    /**
     * 启用规则
     * 
     * @param ruleId 规则ID
     * @param factoryId 车厂ID
     * @return 是否成功
     */
    public boolean enableRule(String ruleId, String factoryId) {
        return ruleService.enableRule(ruleId, factoryId);
    }
    
    /**
     * 禁用规则
     * 
     * @param ruleId 规则ID
     * @param factoryId 车厂ID
     * @return 是否成功
     */
    public boolean disableRule(String ruleId, String factoryId) {
        return ruleService.disableRule(ruleId, factoryId);
    }
    
    /**
     * 清除规则缓存
     */
    public void clearCache() {
        ruleService.clearCache();
        factoryRuleChains.clear();
        LOGGER.info("清除规则缓存完成");
    }
    
    /**
     * 创建规则实例
     * 
     * @param ruleInfo 规则信息
     * @return 规则实例的Optional包装
     */
    public Optional<Rule> createRule(RuleInfo ruleInfo) {
        return ruleService.createRule(ruleInfo);
    }
    
    /**
     * 处理规则更新事件
     * 
     * @param changedRules 变更的规则信息映射
     */
    @Override
    public void onRuleUpdate(Map<String, RuleInfo> changedRules) {
        if (changedRules == null || changedRules.isEmpty()) {
            return;
        }
        
        LOGGER.info("收到规则更新通知，变更规则数量: {}", changedRules.size());
        
        // 清除所有车厂规则链缓存，强制重新创建
        factoryRuleChains.clear();
        
        // 清除规则服务缓存
        ruleService.clearCache();
    }
    
    /**
     * 主程序入口，用于测试
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            // 加载应用配置
            org.battery.quality.config.AppConfig appConfig = org.battery.quality.config.AppConfigLoader.load();
            
            // 初始化数据库连接
            org.battery.quality.config.DatabaseManager.getInstance().initDataSource(appConfig.getMysql());
            
            // 获取规则管理器实例并上传规则
            RuleManager manager = RuleManager.getInstance();
            int count = manager.uploadAllRules();
            System.out.println("成功上传规则数量: " + count);
        } catch (Exception e) {
            System.err.println("规则上传失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭数据库连接
            try {
                org.battery.quality.config.DatabaseManager.getInstance().closeDataSource();
            } catch (Exception e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
} 