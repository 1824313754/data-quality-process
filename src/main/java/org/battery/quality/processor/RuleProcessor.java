package org.battery.quality.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.model.BatteryData;
import org.battery.quality.model.DataStats;
import org.battery.quality.model.ProcessedData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.battery.quality.service.RuleUpdateResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 规则处理器
 * 对数据应用规则检查
 */
@Slf4j
public class RuleProcessor extends KeyedProcessFunction<String, BatteryData, ProcessedData> {

    private static final long serialVersionUID = 1L;

    // 定义侧输出标签，用于输出数据统计信息
    public static final OutputTag<DataStats> STATS_OUTPUT_TAG =
            new OutputTag<DataStats>("data-stats"){};

    // 状态：保存上一条记录
    private transient ValueState<BatteryData> previousDataState;

    // 规则引擎
    private transient RuleEngine ruleEngine;

    // 规则服务
    private transient RuleService ruleService;

    // 定时任务执行器
    private transient ScheduledExecutorService scheduler;

    // 日期时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");


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
        long ruleUpdateIntervalSeconds = appConfig.getDorisRule().getCacheRefreshInterval();
        // 首次加载规则（全量加载）
        initialLoadRules();
        // 启动定时任务，定期增量更新规则
        scheduler =  Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                this::updateRules,
                ruleUpdateIntervalSeconds,
                ruleUpdateIntervalSeconds,
                TimeUnit.SECONDS);

        log.info("规则处理器初始化完成，规则更新间隔: {}秒", ruleUpdateIntervalSeconds);
    }

    /**
     * 初始化加载规则（全量加载）
     */
    private void initialLoadRules() {
        try {
            log.info("开始初始化加载规则...");
            // 清除现有规则
            ruleEngine.clearRules();
            // 全量加载规则
            RuleUpdateResult result = ruleService.updateRules(ruleEngine);
            log.info("规则初始化完成 - {}, 总规则数: {}", result, ruleEngine.getRuleCount());
        } catch (Exception e) {
            log.error("初始化加载规则失败", e);
        }
    }

    /**
     * 增量更新规则
     */
    private void updateRules() {
        try {
            log.debug("开始检查规则更新...");
            // 增量更新规则
            RuleUpdateResult result = ruleService.updateRules(ruleEngine);

            if (result.hasChanges()) {
                log.info("规则更新完成 - {}, 当前规则数: {}", result, ruleEngine.getRuleCount());
            } else {
                log.debug("无规则变更");
            }

            if (result.hasErrors()) {
                log.warn("规则更新过程中发生错误，错误数量: {}", result.errorCount);
            }
        } catch (Exception e) {
            log.error("增量更新规则失败", e);
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
        // 只输出异常数据（有质量问题的数据）
        if (!issues.isEmpty()) {
            ProcessedData result = ProcessedData.builder()
                    .data(data)
                    .issues(issues)
                    .build();
            out.collect(result);
        }

        // 处理数据统计信息
        collectDataStats(data, issues, ctx);
    }

    /**
     * 收集数据统计信息并输出到侧输出流
     *
     * @param data 电池数据
     * @param issues 质量问题列表
     * @param ctx 上下文
     */
    private void collectDataStats(BatteryData data, List<QualityIssue> issues, Context ctx) {
        try {
            // 解析时间
            LocalDateTime dataTime = LocalDateTime.now();
            if (data.getTime() != null) {
                try {
                    dataTime = LocalDateTime.parse(data.getTime(), DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    log.warn("解析数据时间失败: {}", data.getTime());
                }
            }

            // 创建数据统计对象
            DataStats stats = DataStats.builder()
                    .vin(data.getVin())
                    .dayOfYear(dataTime.toLocalDate().format(DATE_FORMATTER))
                    .hour(dataTime.getHour())
                    .vehicleFactory(data.getVehicleFactory())
                    .normalDataCount(issues.isEmpty() ? 1L : 0L)
                    .abnormalDataCount(issues.isEmpty() ? 0L : 1L)
                    .dataCount(1L)
                    .time(dataTime.format(DATE_TIME_FORMATTER))
                    .lastUpdateTime(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                    .build();

            // 输出到侧输出流
            ctx.output(STATS_OUTPUT_TAG, stats);

            if (log.isDebugEnabled()) {
                log.debug("数据统计信息已收集: {}", stats);
            }
        } catch (Exception e) {
            log.error("收集数据统计信息失败", e);
        }
    }
}