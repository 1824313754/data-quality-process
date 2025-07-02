package org.battery.quality.processor;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Gb32960DataWithIssues;
import org.battery.quality.model.Issue;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.Rule;
import org.battery.quality.rule.RuleManager;
import org.battery.quality.rule.StateRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 规则处理器，处理数据并应用质量规则
 */
public class BroadcastRuleProcessor extends KeyedProcessFunction<
        String, Gb32960Data, Gb32960DataWithIssues> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastRuleProcessor.class);
    
    // 车厂规则映射缓存（车厂ID -> 规则列表）
    private final Map<String, List<Rule>> factoryRulesCache = new HashMap<>();
    
    // 车厂状态规则映射缓存（车厂ID -> 状态规则列表）
    private final Map<String, List<StateRule>> factoryStateRulesCache = new HashMap<>();
    
    // 规则实例缓存
    private final Map<String, Rule> ruleInstanceCache = new ConcurrentHashMap<>();
    
    // 保存上一条记录的状态
    private transient ValueState<Gb32960Data> previousDataState;
    
    // 定时任务执行器
    private transient ScheduledExecutorService scheduler;
    
    // 规则信息缓存
    private Map<String, RuleInfo> ruleInfoCache = new HashMap<>();
    
    // 默认车厂ID
    private static final String DEFAULT_FACTORY_ID = "0";
    
    /**
     * 构造函数
     */
    public BroadcastRuleProcessor() {
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        // 创建状态描述符
        ValueStateDescriptor<Gb32960Data> descriptor = 
                new ValueStateDescriptor<>("previous-data", Gb32960Data.class);
        
        // 获取状态
        previousDataState = getRuntimeContext().getState(descriptor);
        
        // 加载应用配置
        AppConfig appConfig = AppConfigLoader.load();
        
        // 初始化数据库连接
        DatabaseManager.getInstance().initDataSource(appConfig.getMysql());
        
        // 获取规则更新间隔（秒）
        long ruleUpdateIntervalSeconds = appConfig.getMysql().getCacheRefreshInterval();
        
        // 初始化规则
        updateRules();
        
        // 启动定时更新规则任务
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::updateRules, 
                ruleUpdateIntervalSeconds, 
                ruleUpdateIntervalSeconds, 
                TimeUnit.SECONDS);
        
        LOGGER.info("规则处理器初始化完成，将每 {} 秒更新一次规则", ruleUpdateIntervalSeconds);
    }
    
    /**
     * 更新规则
     */
    private void updateRules() {
        try {
            LOGGER.info("开始更新规则...");
            
            // 从数据库加载最新规则
            Map<String, RuleInfo> newRuleInfos = RuleManager.loadAllRules();
            
            // 清除旧的规则缓存
            ruleInstanceCache.clear();
            factoryRulesCache.clear();
            factoryStateRulesCache.clear();
            
            // 更新规则信息缓存
            ruleInfoCache = newRuleInfos;
            
            // 编译所有规则
            List<Rule> allRules = new ArrayList<>();
            Map<String, Rule> ruleInstances = new HashMap<>();
            
            for (RuleInfo ruleInfo : ruleInfoCache.values()) {
                Rule rule = getRuleInstance(ruleInfo);
                if (rule != null) {
                    allRules.add(rule);
                    ruleInstances.put(ruleInfo.getId(), rule);
                }
            }
            LOGGER.info("共编译加载了 {} 个规则实例", allRules.size());
            
            // 获取所有车厂ID
            Set<String> factoryIds = getActiveFactoryIds();
            
            // 为每个车厂构建规则缓存
            for (String factoryId : factoryIds) {
                List<Rule> factoryRules = new ArrayList<>();
                List<StateRule> factoryStateRules = new ArrayList<>();
                
                // 遍历所有规则信息，筛选适用于当前车厂的规则
                for (RuleInfo ruleInfo : ruleInfoCache.values()) {
                    if (ruleInfo.isEnabledForFactory(factoryId) || ruleInfo.isEnabledForFactory(DEFAULT_FACTORY_ID)) {
                        Rule rule = ruleInstances.get(ruleInfo.getId());
                        if (rule != null) {
                            factoryRules.add(rule);
                            
                            // 如果是状态规则，也添加到状态规则列表
                            if (rule instanceof StateRule) {
                                factoryStateRules.add((StateRule) rule);
                            }
                        }
                    }
                }
                
                // 保存到缓存
                factoryRulesCache.put(factoryId, factoryRules);
                factoryStateRulesCache.put(factoryId, factoryStateRules);
                
                LOGGER.debug("车厂 {} 规则缓存构建完成，普通规则 {} 个，状态规则 {} 个",
                        factoryId, factoryRules.size(), factoryStateRules.size());
            }
            
            LOGGER.info("规则更新完成，共为 {} 个车厂构建了规则缓存", factoryIds.size());
        } catch (Exception e) {
            LOGGER.error("更新规则失败", e);
        }
    }
    
    /**
     * 从规则信息缓存中提取所有活跃的车厂ID
     * @return 车厂ID集合
     */
    private Set<String> getActiveFactoryIds() {
        Set<String> factoryIds = new HashSet<>();
        // 添加默认车厂ID
        factoryIds.add(DEFAULT_FACTORY_ID);
        
        // 从已加载的规则信息中提取车厂ID
        for (RuleInfo ruleInfo : ruleInfoCache.values()) {
            String enabledFactories = ruleInfo.getEnabledFactories();
            if (enabledFactories != null && !enabledFactories.isEmpty()) {
                for (String id : enabledFactories.split(",")) {
                    id = id.trim();
                    if (!id.isEmpty() && !id.equals("0")) {  // 跳过默认的"0"
                        factoryIds.add(id);
                    }
                }
            }
        }
        
        LOGGER.info("共获取到 {} 个车厂ID", factoryIds.size());
        return factoryIds;
    }
    
    @Override
    public void processElement(
            Gb32960Data data, 
            Context ctx, 
            Collector<Gb32960DataWithIssues> out) throws Exception {
        
        // 如果VIN为空，跳过检测
        if (data.getVin() == null) {
            return;
        }
        
        // 获取车厂代码
        String vehicleFactory = data.getVehicleFactory();
        
        // 如果没有该车厂的规则缓存，使用默认规则
        if (vehicleFactory == null || vehicleFactory.isEmpty() || !factoryRulesCache.containsKey(vehicleFactory)) {
            vehicleFactory = DEFAULT_FACTORY_ID;
            if (data.getVehicleFactory() != null) {
                LOGGER.debug("未找到车厂 {} 的规则缓存，使用通用规则", data.getVehicleFactory());
            }
        }
        
        // 收集所有发现的问题
        List<Issue> allIssues = new ArrayList<>();
        
        // 1. 获取该车厂的普通规则
        List<Rule> rules = factoryRulesCache.getOrDefault(vehicleFactory, new ArrayList<>());
        
        // 2. 执行所有普通规则检查
        for (Rule rule : rules) {
            List<Issue> issues = rule.check(data);
            if (issues != null && !issues.isEmpty()) {
                allIssues.addAll(issues);
            }
        }
        
        // 3. 获取上一条记录
        Gb32960Data previousData = previousDataState.value();
        
        // 4. 获取该车厂的状态规则
        List<StateRule> stateRules = factoryStateRulesCache.getOrDefault(
                vehicleFactory, new ArrayList<>());
        
        // 5. 执行所有状态规则检查
        for (StateRule rule : stateRules) {
            List<Issue> issues = rule.checkState(data, previousData);
            if (issues != null && !issues.isEmpty()) {
                allIssues.addAll(issues);
            }
        }
        
        // 6. 保存当前记录为下一次的上一条记录
        previousDataState.update(data);
        
        // 7. 无论是否有问题，都返回带有原始数据的结果
        Gb32960DataWithIssues result = Gb32960DataWithIssues.builder()
                .data(data)
                .issues(allIssues)
                .build();
        
        out.collect(result);
    }
    
    /**
     * 获取规则实例，如果不存在则创建
     * @param ruleInfo 规则信息
     * @return 规则实例
     */
    private Rule getRuleInstance(RuleInfo ruleInfo) {
        // 尝试从缓存获取
        Rule rule = ruleInstanceCache.get(ruleInfo.getId());
        if (rule != null) {
            return rule;
        }
        
        // 创建新的规则实例
        rule = RuleManager.createRuleInstance(ruleInfo);
        if (rule != null) {
                        // 缓存规则实例
            ruleInstanceCache.put(ruleInfo.getId(), rule);
        }
        
        return rule;
    }
    
    @Override
    public void close() throws Exception {
        // 关闭定时任务执行器
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        super.close();
    }
} 