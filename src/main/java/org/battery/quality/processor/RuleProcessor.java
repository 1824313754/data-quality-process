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
    
    // 处理计数器
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong issueCount = new AtomicLong(0);
    
    // 上次日志时间
    private transient long lastLogTime;
    
    // 日志输出间隔（毫秒）
    private static final long LOG_INTERVAL = 60000;
    
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
        
        // 初始化计数器
        processedCount.set(0);
        issueCount.set(0);
        lastLogTime = System.currentTimeMillis();
        
        // 首次加载规则
        loadRules();
        
        // 启动定时任务，定期更新规则
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rule-updater");
            t.setDaemon(true);
            return t;
        });
        
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
        
        // 更新计数器
        long processed = processedCount.incrementAndGet();
        issueCount.addAndGet(issues.size());
        
        // 保存当前记录为下一次的上一条记录
        previousDataState.update(data);
        
        // 输出结果
        ProcessedData result = ProcessedData.builder()
                .data(data)
                .issues(issues)
                .build();
        
        out.collect(result);
        
        // 定期输出处理统计信息
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > LOG_INTERVAL) {
            LOGGER.info("处理统计: 已处理 {} 条数据，发现 {} 个质量问题", 
                    processed, issueCount.get());
            lastLogTime = currentTime;
        }
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
                Thread.currentThread().interrupt();
            }
        }
        
        // 输出最终统计信息
        LOGGER.info("处理器关闭，总共处理 {} 条数据，发现 {} 个质量问题", 
                processedCount.get(), issueCount.get());
        
        super.close();
    }
} 