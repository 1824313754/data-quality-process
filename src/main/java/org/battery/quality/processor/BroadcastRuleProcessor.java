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
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleManager;
import org.battery.quality.rule.observer.RuleUpdateObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 广播规则处理器，接收规则广播流和数据流，应用规则处理
 * 使用观察者模式接收规则更新
 */
public class BroadcastRuleProcessor extends KeyedBroadcastProcessFunction<
        String, Gb32960Data, Map<String, RuleInfo>, Gb32960DataWithIssues> implements RuleUpdateObserver {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastRuleProcessor.class);
    
    // 规则广播状态描述符
    private final MapStateDescriptor<String, Map<String, RuleInfo>> ruleStateDescriptor;
    
    // 规则管理器
    private transient RuleManager ruleManager;
    
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
        
        // 获取规则管理器实例
        ruleManager = RuleManager.getInstance();
        
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
        
        // 获取上一条记录
        Gb32960Data previousData = previousDataState.value();
        
        // 使用规则管理器检查数据
        List<Issue> allIssues = ruleManager.checkData(data, previousData);
        
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
    public void processBroadcastElement(
            Map<String, RuleInfo> changedRuleInfos, 
            Context ctx, 
            Collector<Gb32960DataWithIssues> out) throws Exception {
        
        LOGGER.info("收到规则广播更新，变更规则数量: {}", changedRuleInfos.size());
        
        // 更新广播状态
        BroadcastState<String, Map<String, RuleInfo>> broadcastState = 
                ctx.getBroadcastState(ruleStateDescriptor);
        
        // 获取当前广播状态中的规则
        Map<String, RuleInfo> currentRules = broadcastState.get("rules");
        if (currentRules == null) {
            // 如果当前没有规则，直接使用新规则
            broadcastState.put("rules", changedRuleInfos);
        } else {
            // 合并规则
            for (Map.Entry<String, RuleInfo> entry : changedRuleInfos.entrySet()) {
                String ruleId = entry.getKey();
                RuleInfo ruleInfo = entry.getValue();
                
                if (ruleInfo == null) {
                    // 规则被删除
                    currentRules.remove(ruleId);
                } else {
                    // 规则新增或更新
                    currentRules.put(ruleId, ruleInfo);
                }
            }
            
            // 更新广播状态
            broadcastState.put("rules", currentRules);
        }
        
        // 作为观察者，接收规则更新
        onRuleUpdate(changedRuleInfos);
    }
    
    @Override
    public void onRuleUpdate(Map<String, RuleInfo> changedRules) {
        // 清除规则缓存，确保使用最新规则
        if (ruleManager != null) {
            ruleManager.clearCache();
        }
    }
    
    @Override
    public void close() throws Exception {
        super.close();
        
        // 关闭数据库连接
        DatabaseManager.getInstance().closeDataSource();
    }
} 