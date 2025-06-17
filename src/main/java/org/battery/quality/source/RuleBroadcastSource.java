package org.battery.quality.source;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 规则广播源，用于从数据库加载规则信息并广播到所有任务实例
 */
public class RuleBroadcastSource extends RichSourceFunction<Map<String, RuleInfo>> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBroadcastSource.class);
    
    private volatile boolean isRunning = true;
    private final long checkIntervalMillis;
    
    // 缓存上次加载的规则MD5值
    private final Map<String, String> lastRuleMd5Map = new ConcurrentHashMap<>();
    // 缓存上次广播的规则信息
    private Map<String, RuleInfo> lastBroadcastRuleInfos = new HashMap<>();
    // 缓存上次加载的规则版本
    private final Map<String, Integer> lastRuleVersionMap = new ConcurrentHashMap<>();
    
    // 添加成员变量记录是否检测到规则变更
    private boolean hasRuleChangesDetected = false;
    // 记录变更的规则ID
    private final Set<String> changedRuleIds = new HashSet<>();
    
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
    }

    @Override
    public void run(SourceContext<Map<String, RuleInfo>> ctx) throws Exception {
        LOGGER.info("规则广播源启动，检查间隔: {}毫秒", checkIntervalMillis);
        
        while (isRunning) {
            try {
                // 清空变更规则集合
                changedRuleIds.clear();
                hasRuleChangesDetected = false;
                
                // 从数据库加载规则信息
                Map<String, RuleInfo> allRuleInfos = loadRuleInfosFromDatabase();
                LOGGER.info("从数据库加载了 {} 个规则信息", allRuleInfos.size());
                
                // 检查是否有规则变更
                boolean hasChanges = checkRuleChanges(allRuleInfos);
                
                // 如果有变更，则只广播变更的规则
                if (hasChanges) {
                    // 只包含变更的规则
                    Map<String, RuleInfo> changedRuleInfos = new HashMap<>();
                    
                    // 第一次加载时，广播所有规则
                    if (lastBroadcastRuleInfos.isEmpty()) {
                        changedRuleInfos.putAll(allRuleInfos);
                        LOGGER.info("首次加载，广播所有 {} 个规则", changedRuleInfos.size());
                    } else {
                        // 非首次加载，只广播变更的规则
                        for (String ruleId : changedRuleIds) {
                            if (allRuleInfos.containsKey(ruleId)) {
                                changedRuleInfos.put(ruleId, allRuleInfos.get(ruleId));
                            }
                        }
                        
                        // 检查是否有规则被删除
                        for (String ruleId : lastBroadcastRuleInfos.keySet()) {
                            if (!allRuleInfos.containsKey(ruleId)) {
                                // 发送null值表示规则已删除
                                changedRuleInfos.put(ruleId, null);
                                LOGGER.info("规则 {} 已删除，广播null值", ruleId);
                            }
                        }
                        
                        LOGGER.info("检测到 {} 个规则变更，进行广播", changedRuleInfos.size());
                    }
                    
                    // 只有在有变更规则时才广播
                    if (!changedRuleInfos.isEmpty()) {
                        synchronized (ctx.getCheckpointLock()) {
                            ctx.collect(changedRuleInfos);
                        }
                        
                        // 更新上次广播的规则信息
                        Map<String, RuleInfo> newBroadcastRuleInfos = new HashMap<>(lastBroadcastRuleInfos);
                        
                        // 更新变更的规则
                        for (Map.Entry<String, RuleInfo> entry : changedRuleInfos.entrySet()) {
                            if (entry.getValue() == null) {
                                // 如果是null值，表示规则已删除
                                newBroadcastRuleInfos.remove(entry.getKey());
                            } else {
                                // 更新或添加规则
                                newBroadcastRuleInfos.put(entry.getKey(), entry.getValue());
                            }
                        }
                        
                        lastBroadcastRuleInfos = newBroadcastRuleInfos;
                    }
                } else {
                    LOGGER.info("规则无变更，跳过广播");
                }
                
                // 等待下次检查
                Thread.sleep(checkIntervalMillis);
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
    
    /**
     * 检查规则是否有变更
     * @param newRuleInfos 新加载的规则信息
     * @return 是否有变更
     */
    private boolean checkRuleChanges(Map<String, RuleInfo> newRuleInfos) {
        // 如果第一次加载，则认为有变更
        if (lastBroadcastRuleInfos.isEmpty()) {
            return true;
        }
        
        // 如果在加载过程中已经检测到MD5或版本变更，直接返回true
        if (hasRuleChangesDetected) {
            return true;
        }
        
        // 比较规则数量
        if (lastBroadcastRuleInfos.size() != newRuleInfos.size()) {
            LOGGER.info("规则数量变化: {} -> {}", lastBroadcastRuleInfos.size(), newRuleInfos.size());
            
            // 检查哪些规则被移除
            for (String ruleId : lastBroadcastRuleInfos.keySet()) {
                if (!newRuleInfos.containsKey(ruleId)) {
                    LOGGER.info("规则被移除: {}", ruleId);
                    changedRuleIds.add(ruleId);
                }
            }
            
            // 检查哪些规则被新增
            for (String ruleId : newRuleInfos.keySet()) {
                if (!lastBroadcastRuleInfos.containsKey(ruleId)) {
                    LOGGER.info("新增规则: {}", ruleId);
                    changedRuleIds.add(ruleId);
                }
            }
            
            return true;
        }
        
        // 没有规则数量变化，检查规则内容是否变更
        boolean hasChanges = false;
        
        // 检查每个规则的版本和MD5是否有变化
        for (String ruleId : newRuleInfos.keySet()) {
            RuleInfo newInfo = newRuleInfos.get(ruleId);
            RuleInfo oldInfo = lastBroadcastRuleInfos.get(ruleId);
            
            if (oldInfo != null) {
                // 检查版本是否变更
                if (newInfo.getVersion() != oldInfo.getVersion()) {
                    LOGGER.info("规则 {} 版本更新: {} -> {}", 
                            ruleId, oldInfo.getVersion(), newInfo.getVersion());
                    changedRuleIds.add(ruleId);
                    hasChanges = true;
                    continue;
                }
                
                // 检查MD5是否变更
                String newMd5 = newInfo.getMd5Hash();
                String oldMd5 = oldInfo.getMd5Hash();
                
                if ((newMd5 == null && oldMd5 != null) || 
                    (newMd5 != null && oldMd5 == null) ||
                    (newMd5 != null && oldMd5 != null && !newMd5.equals(oldMd5))) {
                    LOGGER.info("规则 {} MD5变更: {} -> {}", ruleId, oldMd5, newMd5);
                    changedRuleIds.add(ruleId);
                    hasChanges = true;
                }
            }
        }
        
        return hasChanges;
    }
    
    /**
     * 从数据库加载规则信息
     */
    private Map<String, RuleInfo> loadRuleInfosFromDatabase() throws SQLException {
        // 创建规则信息映射
        Map<String, RuleInfo> ruleInfos = new HashMap<>();
        
        // 从数据库读取MD5和版本信息
        Map<String, String> newRuleMd5Map = new HashMap<>();
        Map<String, Integer> newRuleVersionMap = new HashMap<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT r1.id, r1.name, r1.source_code, r1.md5_hash, r1.version " +
                     "FROM rule_class r1 " +
                     "JOIN (SELECT id, MAX(version) as max_version FROM rule_class GROUP BY id) r2 " +
                     "ON r1.id = r2.id AND r1.version = r2.max_version")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String sourceCode = rs.getString("source_code");
                    String md5Hash = rs.getString("md5_hash");
                    int version = rs.getInt("version");
                    
                    // 创建规则信息对象
                    RuleInfo ruleInfo = new RuleInfo(id, name, sourceCode, version, md5Hash);
                    ruleInfos.put(id, ruleInfo);
                    
                    // 存储规则版本号
                    newRuleVersionMap.put(id, version);
                    
                    // 存储规则MD5值
                    if (md5Hash != null) {
                        newRuleMd5Map.put(id, md5Hash);
                    }
                    
                    // 检查MD5值是否变更
                    if (md5Hash != null && lastRuleMd5Map.containsKey(id)) {
                        String oldMd5 = lastRuleMd5Map.get(id);
                        boolean md5Changed = !md5Hash.equals(oldMd5);
                        if (md5Changed) {
                            LOGGER.info("规则 {} MD5变更: {} -> {}, 版本: {}", 
                                    id, oldMd5, md5Hash, version);
                            hasRuleChangesDetected = true;
                            changedRuleIds.add(id);
                        }
                    } else if (md5Hash == null && lastRuleMd5Map.containsKey(id)) {
                        // 如果之前有MD5值，现在为空，也认为有变更
                        LOGGER.info("规则 {} MD5变为NULL, 版本: {}", id, version);
                        hasRuleChangesDetected = true;
                        changedRuleIds.add(id);
                    } else if (md5Hash != null && !lastRuleMd5Map.containsKey(id)) {
                        // 如果之前没有MD5值，现在有了，也认为有变更
                        LOGGER.info("规则 {} 首次获取MD5: {}, 版本: {}", 
                                id, md5Hash, version);
                        hasRuleChangesDetected = true;
                        changedRuleIds.add(id);
                    }
                    
                    // 检查版本是否变更
                    if (lastRuleVersionMap.containsKey(id) && lastRuleVersionMap.get(id) != version) {
                        LOGGER.info("规则 {} 版本更新: {} -> {}", 
                                id, lastRuleVersionMap.get(id), version);
                        hasRuleChangesDetected = true;
                        changedRuleIds.add(id);
                    }
                }
            }
        }
        
        // 更新缓存
        lastRuleMd5Map.clear();
        lastRuleMd5Map.putAll(newRuleMd5Map);
        
        lastRuleVersionMap.clear();
        lastRuleVersionMap.putAll(newRuleVersionMap);
        
        // 如果检测到规则变更，清除规则缓存
        if (hasRuleChangesDetected) {
            LOGGER.info("检测到规则变更，清除规则缓存");
            RuleManager.clearRuleCache();
        }
        
        return ruleInfos;
    }
} 