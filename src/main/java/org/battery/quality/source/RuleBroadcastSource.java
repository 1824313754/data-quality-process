package org.battery.quality.source;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.service.DefaultRuleService;
import org.battery.quality.rule.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 规则广播源
 * 负责从数据库定期加载规则变更并广播到Flink任务
 */
public class RuleBroadcastSource extends RichSourceFunction<Map<String, RuleInfo>> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBroadcastSource.class);
    
    private volatile boolean isRunning = true;
    private final long checkIntervalMillis;
    
    // 上次检查时间
    private long lastCheckTime = 0;
    
    // 规则服务
    private transient RuleService ruleService;
    
    /**
     * 构造函数
     * @param checkIntervalSeconds 检查数据库规则更新的间隔（秒）
     */
    public RuleBroadcastSource(long checkIntervalSeconds) {
        this.checkIntervalMillis = TimeUnit.SECONDS.toMillis(checkIntervalSeconds);
    }

    @Override
    public void open(Configuration parameters) {
        // 创建规则服务
        this.ruleService = new DefaultRuleService();
        LOGGER.info("规则广播源初始化完成");
    }

    @Override
    public void run(SourceContext<Map<String, RuleInfo>> ctx) throws Exception {
        LOGGER.info("规则广播源启动，检查间隔: {}毫秒", checkIntervalMillis);
        // 首次加载所有规则
        broadcastAllRules(ctx);
        // 进入规则检查循环
        while (isRunning) {
            try {
                // 休眠到下次检查时间
                Thread.sleep(checkIntervalMillis);
                // 检查规则变更并广播
                checkAndBroadcastChanges(ctx);

            } catch (Exception e) {
                LOGGER.error("规则变更检查异常", e);
                // 出错后等待短暂时间再重试
                Thread.sleep(5000);
            }
        }
    }

    /**
     * 广播所有规则
     */
    private void broadcastAllRules(SourceContext<Map<String, RuleInfo>> ctx) {
        try {
            Map<String, RuleInfo> allRules = ruleService.getAllRules();
            if (!allRules.isEmpty()) {
                // 广播所有规则
                synchronized (ctx.getCheckpointLock()) {
                    ctx.collect(allRules);
                }
                lastCheckTime = System.currentTimeMillis();
                LOGGER.info("首次加载完成，广播 {} 个规则", allRules.size());
            } else {
                LOGGER.info("首次加载未发现规则");
            }
        } catch (Exception e) {
            LOGGER.error("加载所有规则失败", e);
        }
    }

    /**
     * 检查规则变更并广播
     */
    private void checkAndBroadcastChanges(SourceContext<Map<String, RuleInfo>> ctx) {
        try {
            // 获取自上次检查以来的规则变更
            Map<String, RuleInfo> changedRules = ruleService.getRuleChanges(lastCheckTime);
            lastCheckTime = System.currentTimeMillis();

            if (!changedRules.isEmpty()) {
                LOGGER.info("检测到 {} 个规则变更，进行广播", changedRules.size());

                // 广播变更的规则
                synchronized (ctx.getCheckpointLock()) {
                    ctx.collect(changedRules);
                }
            } else {
                LOGGER.debug("规则无变更，跳过广播");
            }
        } catch (Exception e) {
            LOGGER.error("检查规则变更失败", e);
        }
    }

    @Override
    public void cancel() {
        LOGGER.info("规则广播源取消");
        isRunning = false;
    }
} 