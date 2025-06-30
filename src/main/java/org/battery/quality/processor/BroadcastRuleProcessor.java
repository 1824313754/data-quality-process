package org.battery.quality.processor;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Gb32960DataWithIssues;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.Rule;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.StateRule;
import org.battery.quality.util.DynamicCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 广播规则处理器，接收规则广播流和数据流，应用规则处理
 */
public class BroadcastRuleProcessor extends KeyedBroadcastProcessFunction<
        String, Gb32960Data, Map<String, RuleInfo>, Gb32960DataWithIssues> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastRuleProcessor.class);
    
    // 规则广播状态描述符
    private final MapStateDescriptor<String, Map<String, RuleInfo>> ruleStateDescriptor;
    
    // 车厂规则映射缓存（车厂ID -> 规则列表）
    private final Map<String, List<Rule>> factoryRulesCache = new HashMap<>();
    
    // 车厂状态规则映射缓存（车厂ID -> 状态规则列表）
    private final Map<String, List<StateRule>> factoryStateRulesCache = new HashMap<>();
    
    // 规则实例缓存，避免重复编译，key为"规则ID:版本号"
    private final Map<String, Rule> ruleInstanceCache = new ConcurrentHashMap<>();
    
    // 保存上一条记录的状态
    private transient ValueState<Gb32960Data> previousDataState;
    
    /**
     * 构造函数
     * @param ruleStateDescriptor 规则广播状态描述符
     */
    public BroadcastRuleProcessor(
            MapStateDescriptor<String, Map<String, RuleInfo>> ruleStateDescriptor) {
        this.ruleStateDescriptor = ruleStateDescriptor;
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
        LOGGER.info("广播规则处理器初始化完成");
    }
    
    @Override
    public void processElement(
            Gb32960Data data, 
            ReadOnlyContext ctx, 
            Collector<Gb32960DataWithIssues> out) throws Exception {
        
        // 如果VIN为空，跳过检测
        if (data.getVin() == null) {
            return;
        }
        
        // 获取车厂代码
        String vehicleFactory = data.getVehicleFactory();
        
        // 从广播状态获取规则
        ReadOnlyBroadcastState<String, Map<String, RuleInfo>> broadcastState = 
                ctx.getBroadcastState(ruleStateDescriptor);
        
        // 检查规则状态是否存在
        if (!broadcastState.contains("rules")) {
            LOGGER.debug("规则广播状态尚未初始化，跳过处理");
            return;
        }
        
        // 如果尚未为该车厂缓存规则，则构建缓存
        if (!factoryRulesCache.containsKey(vehicleFactory)) {
            buildRuleCaches(vehicleFactory);
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
    
    @Override
    public void processBroadcastElement(
            Map<String, RuleInfo> changedRuleInfos, 
            Context ctx, 
            Collector<Gb32960DataWithIssues> out) throws Exception {
        
        LOGGER.info("收到规则广播更新，变更规则数量: {}", changedRuleInfos.size());
        
        // 更新广播状态
        BroadcastState<String, Map<String, RuleInfo>> broadcastState = 
                ctx.getBroadcastState(ruleStateDescriptor);
        
        // 获取当前广播状态中的规则
        Map<String, RuleInfo> currentRules = new HashMap<>();
        if (broadcastState.contains("rules")) {
            Map<String, RuleInfo> rules = broadcastState.get("rules");
            if (rules != null) {
                currentRules.putAll(rules);
            }
        }
        
        // 处理规则变更
        boolean hasRuleChanges = false;
        
        // 1. 处理变更的规则
        for (Map.Entry<String, RuleInfo> entry : changedRuleInfos.entrySet()) {
            String ruleId = entry.getKey();
            RuleInfo ruleInfo = entry.getValue();
            
            if (ruleInfo == null) {
                // 规则被删除
                currentRules.remove(ruleId);
                
                // 从规则实例缓存中移除该规则所有版本的实例
                List<String> keysToRemove = new ArrayList<>();
                for (String key : ruleInstanceCache.keySet()) {
                    if (key.startsWith(ruleId + ":")) {
                        keysToRemove.add(key);
                    }
                }
                
                for (String key : keysToRemove) {
                    ruleInstanceCache.remove(key);
                    LOGGER.info("从缓存移除规则实例: {}", key);
                }
                
                hasRuleChanges = true;
            } else {
                // 规则新增或更新
                currentRules.put(ruleId, ruleInfo);
                
                // 构造缓存键
                String cacheKey = ruleId + ":" + ruleInfo.getVersion();
                
                // 判断是否需要重新编译规则
                boolean needRecompile = true;
                
                // 如果是同版本规则，检查缓存是否已存在，并比较MD5
                for (String key : ruleInstanceCache.keySet()) {
                    if (key.equals(cacheKey)) {
                        // 已有相同版本规则，直接跳过
                        LOGGER.info("规则 {} 版本 {} 已存在缓存中，跳过编译", ruleId, ruleInfo.getVersion());
                        needRecompile = false;
                        break;
                    }
                }
                
                // 删除旧版本的规则实例
                if (needRecompile) {
                    List<String> keysToRemove = new ArrayList<>();
                    for (String key : ruleInstanceCache.keySet()) {
                        if (key.startsWith(ruleId + ":") && !key.equals(cacheKey)) {
                            keysToRemove.add(key);
                        }
                    }
                    
                    for (String key : keysToRemove) {
                        ruleInstanceCache.remove(key);
                        LOGGER.info("从缓存移除旧版本规则实例: {}", key);
                    }
                }
                
                // 只有需要重新编译的规则才编译
                if (needRecompile) {
                    try {
                        // 动态编译规则类
                        Class<?> ruleClass = DynamicCompiler.compile(ruleInfo.getName(), ruleInfo.getSourceCode());
                        
                        // 实例化规则
                        Rule rule = (Rule) ruleClass.getDeclaredConstructor().newInstance();
                        
                        // 缓存规则实例
                        ruleInstanceCache.put(cacheKey, rule);
                        
                        LOGGER.info("成功编译并加载规则: ID={}, 类名={}, 版本={}", 
                                ruleInfo.getId(), ruleInfo.getName(), ruleInfo.getVersion());
                        
                        hasRuleChanges = true;
                    } catch (Exception e) {
                        LOGGER.error("动态编译规则失败: ID={}, 类名={}", 
                                ruleInfo.getId(), ruleInfo.getName(), e);
                    }
                }
            }
        }
        
        // 更新广播状态
        broadcastState.put("rules", currentRules);
        
        // 只有规则有变更时才清空车厂规则缓存
        if (hasRuleChanges) {
            // 清空车厂规则缓存，会在下次处理元素时重新构建
            factoryRulesCache.clear();
            factoryStateRulesCache.clear();
            
            LOGGER.info("规则有变更，清空车厂规则缓存");
        }
        
        LOGGER.info("规则广播状态更新完成，当前共有规则 {} 个，规则实例缓存 {} 个", 
                currentRules.size(), ruleInstanceCache.size());
    }
    
    /**
     * 为指定车厂构建规则缓存
     * @param factoryId 车厂ID
     */
    private void buildRuleCaches(String factoryId) {
        List<Rule> rules = new ArrayList<>();
        List<StateRule> stateRules = new ArrayList<>();
        
        // 默认所有车厂使用规则
        for (Rule rule : ruleInstanceCache.values()) {
            // 添加到规则列表
            rules.add(rule);
            
            // 如果是状态规则，也添加到状态规则列表
            if (rule instanceof StateRule) {
                stateRules.add((StateRule) rule);
            }
        }
        
        // 按优先级从高到低排序规则（优先级值越大越优先执行）
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        // 按优先级从高到低排序状态规则
        stateRules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        // 缓存规则列表
        factoryRulesCache.put(factoryId, rules);
        factoryStateRulesCache.put(factoryId, stateRules);
        
        LOGGER.debug("为车厂 {} 构建规则缓存，普通规则: {}, 状态规则: {}，已按优先级排序", 
                factoryId, rules.size(), stateRules.size());
    }
} 