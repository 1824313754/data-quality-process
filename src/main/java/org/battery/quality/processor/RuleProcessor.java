package org.battery.quality.processor;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.model.BatteryData;
import org.battery.quality.model.ProcessedData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 规则处理器
 * 对数据应用规则检查
 */
public class RuleProcessor extends KeyedProcessFunction<String, BatteryData, ProcessedData> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleProcessor.class);
    
    // 状态：保存上一条记录
    private transient ValueState<BatteryData> previousDataState;
    
    // 规则引擎
    private transient RuleEngine ruleEngine;
    
    // 规则服务
    private transient RuleService ruleService;
    
    // 定时任务执行器
    private transient ScheduledExecutorService scheduler;

    
    @Override
    public void open(Configuration parameters) throws Exception {
        // 创建状态描述符
        ValueStateDescriptor<BatteryData> descriptor = 
                new ValueStateDescriptor<>("previous-data", BatteryData.class);
        // 获取状态
        previousDataState = getRuntimeContext().getState(descriptor);
        // 创建规则引擎
        ruleEngine = new RuleEngine();
        // 创建规则服务
        ruleService = new RuleService();
        // 加载应用配置
        AppConfig appConfig = ConfigManager.getInstance().getConfig();
        // 获取规则更新间隔（秒）
        long ruleUpdateIntervalSeconds = appConfig.getMysql().getCacheRefreshInterval();
        // 首次加载规则
        loadRules();
        // 启动定时任务，定期更新规则
        scheduler =  Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                this::loadRules, 
                ruleUpdateIntervalSeconds, 
                ruleUpdateIntervalSeconds, 
                TimeUnit.SECONDS);
        
        LOGGER.info("规则处理器初始化完成，规则更新间隔: {}秒", ruleUpdateIntervalSeconds);
    }
    
    /**
     * 加载规则
     */
    private void loadRules() {
        try {
            LOGGER.info("开始加载规则...");
            // 清除现有规则
            ruleEngine.clearRules();
            // 加载规则
            ruleService.loadRules(ruleEngine);
            LOGGER.info("规则加载完成，共加载 {} 条规则", ruleEngine.getRuleCount());
        } catch (Exception e) {
            LOGGER.error("加载规则失败", e);
        }
    }
    
    @Override
    public void processElement(
            BatteryData data,
            Context ctx,
            Collector<ProcessedData> out) throws Exception {
        // 如果VIN为空，跳过处理
        if (data.getVin() == null) {
            return;
        }
        // 获取车厂ID
        String vehicleFactory = data.getVehicleFactory();
        // 获取上一条记录
        BatteryData previousData = previousDataState.value();
        // 应用规则检查
        List<QualityIssue> issues = ruleEngine.checkData(data, previousData, vehicleFactory);
        // 保存当前记录为下一次的上一条记录
        previousDataState.update(data);
        // 输出结果
        ProcessedData result = ProcessedData.builder()
                .data(data)
                .issues(issues)
                .build();
        out.collect(result);
    }
    

} 