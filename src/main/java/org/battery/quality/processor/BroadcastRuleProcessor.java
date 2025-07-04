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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 规则处理器，处理数据并应用质量规则
 */
public class BroadcastRuleProcessor extends KeyedProcessFunction<
        String, Gb32960Data, Gb32960DataWithIssues> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastRuleProcessor.class);
    
    // 保存上一条记录的状态
    private transient ValueState<Gb32960Data> previousDataState;
    
    // 定时任务执行器
    private transient ScheduledExecutorService scheduler;
    
    // 默认车厂ID
    private static final String DEFAULT_FACTORY_ID = "0";
    
    // 缓存的规则集合（按车厂分组）
    private final AtomicReference<Map<String, List<Rule>>> ruleCache = new AtomicReference<>(new HashMap<>());
    
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
            
            // 从数据库加载最新规则信息
            Map<String, RuleInfo> ruleInfos = RuleManager.loadAllRules();
            
            // 创建新的规则缓存
            Map<String, List<Rule>> newRuleCache = new HashMap<>();
            
            // 初始化默认车厂规则列表
            newRuleCache.put(DEFAULT_FACTORY_ID, new ArrayList<>());
            
            // 处理每条规则信息
            for (RuleInfo ruleInfo : ruleInfos.values()) {
                // 创建规则实例
                Rule rule = RuleManager.createRuleInstance(ruleInfo);
                if (rule == null) {
                    continue;
                }
                
                // 获取规则适用的车厂列表
                String enabledFactories = ruleInfo.getEnabledFactories();
                if (enabledFactories == null || enabledFactories.isEmpty()) {
                    // 如果没有指定车厂，添加到默认车厂规则列表
                    newRuleCache.get(DEFAULT_FACTORY_ID).add(rule);
                    continue;
                }
                
                // 处理规则适用的每个车厂
                for (String factoryId : enabledFactories.split(",")) {
                    factoryId = factoryId.trim();
                    if (factoryId.isEmpty()) {
                        continue;
                    }
                    
                    // 如果是默认车厂ID，添加到默认车厂规则列表
                    if (factoryId.equals(DEFAULT_FACTORY_ID)) {
                        newRuleCache.get(DEFAULT_FACTORY_ID).add(rule);
                        continue;
                    }
                    
                    // 如果车厂不存在于缓存中，创建新的规则列表
                    if (!newRuleCache.containsKey(factoryId)) {
                        newRuleCache.put(factoryId, new ArrayList<>());
                    }
                    
                    // 添加规则到车厂规则列表
                    newRuleCache.get(factoryId).add(rule);
                }
            }
            
            // 更新规则缓存
            ruleCache.set(newRuleCache);
            
            LOGGER.info("规则更新完成，共加载 {} 个车厂的规则", newRuleCache.size());
        } catch (Exception e) {
            LOGGER.error("更新规则失败", e);
        }
    }
    
    /**
     * 获取适用于指定车厂的规则列表
     * @param factoryId 车厂ID
     * @return 规则列表
     */
    private List<Rule> getRulesForFactory(String factoryId) {
        List<Rule> result = new ArrayList<>();
        Map<String, List<Rule>> currentCache = ruleCache.get();
        
        // 添加默认规则
        List<Rule> defaultRules = currentCache.get(DEFAULT_FACTORY_ID);
        if (defaultRules != null) {
            result.addAll(defaultRules);
        }
        
        // 如果不是默认车厂，添加车厂特定规则
        if (!DEFAULT_FACTORY_ID.equals(factoryId)) {
            List<Rule> factoryRules = currentCache.get(factoryId);
            if (factoryRules != null) {
                result.addAll(factoryRules);
            }
        }
        
        return result;
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
        if (vehicleFactory == null || vehicleFactory.isEmpty()) {
            vehicleFactory = DEFAULT_FACTORY_ID;
        }
        
        // 收集所有发现的问题
        List<Issue> allIssues = new ArrayList<>();
        
        // 获取适用于当前车厂的规则
        List<Rule> rules = getRulesForFactory(vehicleFactory);
        
        // 获取上一条记录
        Gb32960Data previousData = previousDataState.value();
        
        // 执行规则检查
        for (Rule rule : rules) {
            if (rule instanceof StateRule) {
                // 执行状态规则检查
                List<Issue> issues = ((StateRule) rule).checkState(data, previousData);
                if (issues != null && !issues.isEmpty()) {
                    allIssues.addAll(issues);
                }
            } else {
                // 执行普通规则检查
                List<Issue> issues = rule.check(data);
                if (issues != null && !issues.isEmpty()) {
                    allIssues.addAll(issues);
                }
            }
        }
        
        // 保存当前记录为下一次的上一条记录
        previousDataState.update(data);
        
        // 无论是否有问题，都返回带有原始数据的结果
        Gb32960DataWithIssues result = Gb32960DataWithIssues.builder()
                .data(data)
                .issues(allIssues)
                .build();
        
        out.collect(result);
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