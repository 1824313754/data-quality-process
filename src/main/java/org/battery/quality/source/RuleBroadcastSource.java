package org.battery.quality.source;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleManager;
import org.battery.quality.rule.service.DefaultRuleService;
import org.battery.quality.rule.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 规则广播源，用于从数据库加载规则信息并广播到所有任务实例
 * 简化后的实现，利用RuleService和RuleUpdateManager提供的功能
 */
public class RuleBroadcastSource extends RichSourceFunction<Map<String, RuleInfo>> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBroadcastSource.class);
    
    private volatile boolean isRunning = true;
    private final long checkIntervalMillis;
    
    // 记录上次检查时间
    private long lastCheckTime = 0;
    
    // 上次广播的规则信息
    private Map<String, RuleInfo> lastBroadcastRuleInfos = new HashMap<>();
    
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
    public void open(Configuration parameters) throws Exception {
        // 加载应用配置
        AppConfig appConfig = AppConfigLoader.load();
        // 初始化数据库连接
        DatabaseManager.getInstance().initDataSource(appConfig.getMysql());
        // 创建规则服务
        this.ruleService = new DefaultRuleService();
    }

    @Override
    public void run(SourceContext<Map<String, RuleInfo>> ctx) throws Exception {
        LOGGER.info("规则广播源启动，检查间隔: {}毫秒", checkIntervalMillis);
        
        // 首次加载所有规则
        try {
            Map<String, RuleInfo> allRules = ruleService.getAllRules();
            if (!allRules.isEmpty()) {
                synchronized (ctx.getCheckpointLock()) {
                    ctx.collect(allRules);
                }
                lastBroadcastRuleInfos = new HashMap<>(allRules);
                lastCheckTime = System.currentTimeMillis();
                LOGGER.info("首次加载完成，广播 {} 个规则", allRules.size());
            }
        } catch (Exception e) {
            LOGGER.error("首次加载规则失败", e);
        }
        
        // 定期检查规则变更
        while (isRunning) {
            try {
                // 等待下次检查
                Thread.sleep(checkIntervalMillis);
                
                // 获取自上次检查以来的规则变更
                Map<String, RuleInfo> changedRules = ruleService.getRuleChanges(lastCheckTime);
                lastCheckTime = System.currentTimeMillis();
                
                if (!changedRules.isEmpty()) {
                    LOGGER.info("检测到 {} 个规则变更，进行广播", changedRules.size());
                    
                    // 广播变更的规则
                    synchronized (ctx.getCheckpointLock()) {
                        ctx.collect(changedRules);
                    }
                    
                    // 更新上次广播的规则信息
                    for (Map.Entry<String, RuleInfo> entry : changedRules.entrySet()) {
                        if (entry.getValue() == null) {
                            // 如果是null值，表示规则已删除
                            lastBroadcastRuleInfos.remove(entry.getKey());
                        } else {
                            // 更新或添加规则
                            lastBroadcastRuleInfos.put(entry.getKey(), entry.getValue());
                        }
                    }
                    
                    // 通知规则管理器更新规则缓存
                    RuleManager.getInstance().clearCache();
                } else {
                    LOGGER.info("规则无变更，跳过广播");
                }
            } catch (Exception e) {
                LOGGER.error("规则广播源异常", e);
                // 出错后等待一段时间再重试
                Thread.sleep(5000);
            }
        }
    }
    
    @Override
    public void cancel() {
        LOGGER.info("规则广播源取消");
        isRunning = false;
    }
} 