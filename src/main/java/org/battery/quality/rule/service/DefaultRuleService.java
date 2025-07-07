package org.battery.quality.rule.service;

import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.Rule;
import org.battery.quality.rule.adapter.AdapterRegistry;
import org.battery.quality.rule.adapter.RuleInfoToRuleAdapter;
import org.battery.quality.rule.annotation.QualityRule;
import org.battery.quality.rule.factory.RuleFactoryRegistry;
import org.battery.quality.rule.observer.RuleUpdateManager;
import org.battery.quality.util.DynamicCompiler;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认规则服务实现
 */
public class DefaultRuleService implements RuleService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRuleService.class);
    
    /**
     * 规则实例缓存，key为"规则ID:版本号"
     */
    private final Map<String, Rule> ruleInstanceCache = new ConcurrentHashMap<>();
    
    /**
     * 车厂规则缓存，key为车厂ID
     */
    private final Map<String, List<Rule>> factoryRulesCache = new ConcurrentHashMap<>();
    
    /**
     * 适配器注册表
     */
    private final AdapterRegistry adapterRegistry;
    
    /**
     * 规则工厂注册表
     */
    private final RuleFactoryRegistry factoryRegistry;
    
    /**
     * 规则更新管理器
     */
    private final RuleUpdateManager updateManager;
    
    /**
     * 构造函数
     */
    public DefaultRuleService() {
        this.adapterRegistry = AdapterRegistry.getInstance();
        this.factoryRegistry = RuleFactoryRegistry.getInstance();
        this.updateManager = RuleUpdateManager.getInstance();
    }
    
    /**
     * 构造函数，指定组件
     * 
     * @param adapterRegistry 适配器注册表
     * @param factoryRegistry 工厂注册表
     * @param updateManager 更新管理器
     */
    public DefaultRuleService(AdapterRegistry adapterRegistry, 
            RuleFactoryRegistry factoryRegistry, 
            RuleUpdateManager updateManager) {
        this.adapterRegistry = adapterRegistry != null ? 
                adapterRegistry : AdapterRegistry.getInstance();
        this.factoryRegistry = factoryRegistry != null ? 
                factoryRegistry : RuleFactoryRegistry.getInstance();
        this.updateManager = updateManager != null ? 
                updateManager : RuleUpdateManager.getInstance();
    }
    
    /**
     * 确保数据库连接已初始化
     * 如果数据库连接尚未初始化，则初始化它
     */
    private void ensureDatabaseConnected() {
        try {
            if (!DatabaseManager.getInstance().isDataSourceInitialized()) {
                LOGGER.info("数据库连接未初始化，正在初始化...");
                AppConfig appConfig = AppConfigLoader.load();
                DatabaseManager.getInstance().initDataSource(appConfig.getMysql());
                LOGGER.info("数据库连接初始化完成");
            }
        } catch (Exception e) {
            LOGGER.error("初始化数据库连接失败", e);
            throw new RuntimeException("初始化数据库连接失败", e);
        }
    }
    
    @Override
    public int uploadAllRules() {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        int successCount = 0;
        
        try {
            LOGGER.info("开始上传规则...");
            
            // 使用Reflections库扫描包中的类
            Reflections reflections = new Reflections("org.battery.quality.rule.impl");
            Set<Class<?>> ruleClasses = reflections.getTypesAnnotatedWith(QualityRule.class);
            
            for (Class<?> clazz : ruleClasses) {
                // 跳过接口、抽象类和内部类
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())
                        || clazz.isMemberClass() || clazz.isAnonymousClass()) {
                    continue;
                }
                
                QualityRule annotation = clazz.getAnnotation(QualityRule.class);
                if (!annotation.enabled()) {
                    LOGGER.info("规则 {} 已禁用，跳过上传", clazz.getName());
                    continue;
                }
                
                try {
                    // 读取源代码
                    String sourceCode = readSourceCode(clazz);
                    
                    // 如果有源代码，计算MD5哈希值
                    String md5Hash = null;
                    if (sourceCode != null && !sourceCode.isEmpty()) {
                        md5Hash = calculateMd5(sourceCode);
                        
                        // 上传到数据库
                        int result = saveRuleToDatabase(clazz.getName(), annotation, sourceCode, md5Hash);
                        if (result == 1) { // 成功上传
                            successCount++;
                            LOGGER.info("成功上传规则: ID={}, 类名={}, MD5: {}", 
                                annotation.type(), clazz.getName(), md5Hash);
                        } else if (result == 2) { // MD5相同跳过
                            LOGGER.info("规则源代码未变化，跳过上传: ID={}, 类名={}, MD5: {}", 
                                annotation.type(), clazz.getName(), md5Hash);
                        }
                    } else {
                        LOGGER.warn("无法读取规则类源代码: {}", clazz.getName());
                    }
                } catch (Exception e) {
                    LOGGER.error("上传规则失败: " + clazz.getName(), e);
                }
            }
            
            LOGGER.info("规则上传完成，共上传成功 {} 个规则", successCount);
        } catch (Exception e) {
            LOGGER.error("规则上传过程发生错误", e);
        }
        
        return successCount;
    }
    
    @Override
    public Map<String, RuleInfo> getAllRules() {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        Map<String, RuleInfo> rules = new HashMap<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT rc.id, rc.version, rc.name, rc.description, rc.category, " +
                    "rc.rule_code, rc.priority, rc.source_code, rc.md5_hash, " +
                    "rc.enabled_factories, rc.create_time, rc.update_time " +
                    "FROM rule_class rc " +
                    "INNER JOIN ( " +
                    "  SELECT id, MAX(version) as max_version " +
                    "  FROM rule_class " +
                    "  GROUP BY id " +
                    ") latest ON rc.id = latest.id AND rc.version = latest.max_version")) {
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        RuleInfo ruleInfo = new RuleInfo(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("source_code"),
                                rs.getInt("version"),
                                rs.getString("md5_hash")
                        );
                        
                        rules.put(ruleInfo.getId(), ruleInfo);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("获取规则列表失败", e);
        }
        
        return rules;
    }
    
    @Override
    public Optional<RuleInfo> getRuleById(String ruleId) {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        if (ruleId == null || ruleId.isEmpty()) {
            return Optional.empty();
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT rc.id, rc.version, rc.name, rc.description, rc.category, " +
                    "rc.rule_code, rc.priority, rc.source_code, rc.md5_hash, " +
                    "rc.enabled_factories, rc.create_time, rc.update_time " +
                    "FROM rule_class rc " +
                    "WHERE rc.id = ? " +
                    "ORDER BY rc.version DESC LIMIT 1")) {
                
                stmt.setString(1, ruleId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        RuleInfo ruleInfo = new RuleInfo(
                                rs.getString("id"),
                                rs.getString("name"),
                                rs.getString("source_code"),
                                rs.getInt("version"),
                                rs.getString("md5_hash")
                        );
                        
                        return Optional.of(ruleInfo);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("获取规则信息失败: {}", ruleId, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Rule> getFactoryRules(String factoryId) {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        // 如果缓存中已有该车厂的规则，直接返回
        if (factoryRulesCache.containsKey(factoryId)) {
            return new ArrayList<>(factoryRulesCache.get(factoryId));
        }
        
        List<Rule> factoryRules = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT rc.id, rc.version, rc.name, rc.description, rc.category, " +
                    "rc.rule_code, rc.priority, rc.source_code, rc.md5_hash, " +
                    "rc.enabled_factories, rc.create_time, rc.update_time " +
                    "FROM rule_class rc " +
                    "INNER JOIN ( " +
                    "  SELECT id, MAX(version) as max_version " +
                    "  FROM rule_class " +
                    "  GROUP BY id " +
                    ") latest ON rc.id = latest.id AND rc.version = latest.max_version " +
                    "WHERE rc.enabled_factories = '0' OR rc.enabled_factories LIKE ?")) {
                
                stmt.setString(1, "%," + factoryId + ",%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String ruleId = rs.getString("id");
                        String name = rs.getString("name");
                        String sourceCode = rs.getString("source_code");
                        int version = rs.getInt("version");
                        String md5Hash = rs.getString("md5_hash");
                        
                        // 构造缓存键
                        String cacheKey = ruleId + ":" + version;
                        
                        // 从缓存获取规则实例，如果没有则创建
                        Rule rule = ruleInstanceCache.get(cacheKey);
                        if (rule == null) {
                            RuleInfo ruleInfo = new RuleInfo(ruleId, name, sourceCode, version, md5Hash);
                            Optional<Rule> ruleOpt = createRule(ruleInfo);
                            if (ruleOpt.isPresent()) {
                                rule = ruleOpt.get();
                                ruleInstanceCache.put(cacheKey, rule);
                            }
                        }
                        
                        if (rule != null) {
                            factoryRules.add(rule);
                        }
                    }
                }
            }
            
            // 缓存车厂规则
            factoryRulesCache.put(factoryId, factoryRules);
            
        } catch (SQLException e) {
            LOGGER.error("获取车厂规则失败: {}", factoryId, e);
        }
        
        return factoryRules;
    }
    
    @Override
    public Map<String, RuleInfo> getRuleChanges(long lastUpdateTime) {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        Map<String, RuleInfo> changedRules = new HashMap<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT rc.id, rc.version, rc.name, rc.description, rc.category, " +
                    "rc.rule_code, rc.priority, rc.source_code, rc.md5_hash, " +
                    "rc.enabled_factories, rc.create_time, rc.update_time " +
                    "FROM rule_class rc " +
                    "WHERE rc.update_time > ? " +
                    "ORDER BY rc.id, rc.version DESC")) {
                
                stmt.setTimestamp(1, new Timestamp(lastUpdateTime));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    String currentRuleId = null;
                    
                    while (rs.next()) {
                        String ruleId = rs.getString("id");
                        
                        // 对于每个规则ID，只保留最新版本
                        if (currentRuleId == null || !currentRuleId.equals(ruleId)) {
                            currentRuleId = ruleId;
                            
                            RuleInfo ruleInfo = new RuleInfo(
                                    ruleId,
                                    rs.getString("name"),
                                    rs.getString("source_code"),
                                    rs.getInt("version"),
                                    rs.getString("md5_hash")
                            );
                            
                            changedRules.put(ruleId, ruleInfo);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("获取规则变更失败", e);
        }
        
        return changedRules;
    }
    
    @Override
    public boolean enableRule(String ruleId, String factoryId) {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        if (ruleId == null || ruleId.isEmpty() || factoryId == null || factoryId.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // 获取规则当前的启用车厂列表
            String enabledFactories = null;
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT enabled_factories FROM rule_class " +
                    "WHERE id = ? ORDER BY version DESC LIMIT 1")) {
                stmt.setString(1, ruleId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        enabledFactories = rs.getString("enabled_factories");
                    } else {
                        LOGGER.warn("找不到规则: {}", ruleId);
                        return false;
                    }
                }
            }
            
            // 更新启用车厂列表
            String newEnabledFactories;
            if (enabledFactories == null || enabledFactories.isEmpty() || "0".equals(enabledFactories)) {
                // 如果当前为空或全局启用，则不需要更改
                newEnabledFactories = "0";
            } else {
                // 如果当前已经包含该车厂，则不需要更改
                if (enabledFactories.contains("," + factoryId + ",")) {
                    return true;
                }
                
                // 添加车厂ID
                newEnabledFactories = enabledFactories + factoryId + ",";
            }
            
            // 更新数据库
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE rule_class SET enabled_factories = ? " +
                    "WHERE id = ? AND version = (SELECT MAX(version) FROM rule_class WHERE id = ?)")) {
                stmt.setString(1, newEnabledFactories);
                stmt.setString(2, ruleId);
                stmt.setString(3, ruleId);
                
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    // 清除车厂规则缓存
                    factoryRulesCache.remove(factoryId);
                    LOGGER.info("启用规则 {} 成功，车厂: {}", ruleId, factoryId);
                    
                    // 通知观察者
                    Optional<RuleInfo> ruleInfo = getRuleById(ruleId);
                    ruleInfo.ifPresent(info -> {
                        updateManager.updateRule(ruleId, info);
                        updateManager.notifyObservers();
                    });
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("启用规则失败: {}, 车厂: {}", ruleId, factoryId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean disableRule(String ruleId, String factoryId) {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        if (ruleId == null || ruleId.isEmpty() || factoryId == null || factoryId.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // 获取规则当前的启用车厂列表
            String enabledFactories = null;
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT enabled_factories FROM rule_class " +
                    "WHERE id = ? ORDER BY version DESC LIMIT 1")) {
                stmt.setString(1, ruleId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        enabledFactories = rs.getString("enabled_factories");
                    } else {
                        LOGGER.warn("找不到规则: {}", ruleId);
                        return false;
                    }
                }
            }
            
            // 更新启用车厂列表
            String newEnabledFactories;
            if (enabledFactories == null || enabledFactories.isEmpty() || "0".equals(enabledFactories)) {
                // 如果当前为全局启用，则需要列出除了要禁用的车厂外的所有车厂
                newEnabledFactories = getAllFactoriesExcept(factoryId);
            } else {
                // 如果当前已经不包含该车厂，则不需要更改
                if (!enabledFactories.contains("," + factoryId + ",")) {
                    return true;
                }
                
                // 移除车厂ID
                newEnabledFactories = enabledFactories.replace("," + factoryId + ",", ",");
                if (newEnabledFactories.equals(",")) {
                    newEnabledFactories = "";
                }
            }
            
            // 更新数据库
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE rule_class SET enabled_factories = ? " +
                    "WHERE id = ? AND version = (SELECT MAX(version) FROM rule_class WHERE id = ?)")) {
                stmt.setString(1, newEnabledFactories);
                stmt.setString(2, ruleId);
                stmt.setString(3, ruleId);
                
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    // 清除车厂规则缓存
                    factoryRulesCache.remove(factoryId);
                    LOGGER.info("禁用规则 {} 成功，车厂: {}", ruleId, factoryId);
                    
                    // 通知观察者
                    Optional<RuleInfo> ruleInfo = getRuleById(ruleId);
                    ruleInfo.ifPresent(info -> {
                        updateManager.updateRule(ruleId, info);
                        updateManager.notifyObservers();
                    });
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("禁用规则失败: {}, 车厂: {}", ruleId, factoryId, e);
        }
        
        return false;
    }
    
    @Override
    public Optional<Rule> createRule(RuleInfo ruleInfo) {
        if (ruleInfo == null) {
            return Optional.empty();
        }
        
        // 使用规则工厂创建规则
        Rule rule = factoryRegistry.createRule(ruleInfo);
        if (rule != null) {
            return Optional.of(rule);
        }
        
        // 使用适配器作为备选方案
        rule = adapterRegistry.adapt(ruleInfo);
        return Optional.ofNullable(rule);
    }
    
    @Override
    public void clearCache() {
        ruleInstanceCache.clear();
        factoryRulesCache.clear();
        LOGGER.info("清除规则缓存完成");
    }
    
    /**
     * 保存规则到数据库
     * 
     * @param className 类名
     * @param annotation 规则注解
     * @param sourceCode 源代码
     * @param md5Hash MD5哈希值
     * @return 保存结果：0-失败，1-成功上传，2-MD5相同跳过
     */
    private int saveRuleToDatabase(String className, QualityRule annotation, 
            String sourceCode, String md5Hash) {
        // 确保数据库连接已初始化
        ensureDatabaseConnected();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // 获取规则ID
            String ruleId = annotation.type();
            int ruleCode = annotation.code();
            LOGGER.debug("处理规则: ID={}, 类名={}, 异常编码={}", ruleId, className, ruleCode);
            
            // 检查规则是否已存在及其最新版本信息
            boolean exists = false;
            int latestVersion = 0;
            String existingMd5 = "";
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, MAX(version) as latest_version, " +
                    "(SELECT md5_hash FROM rule_class WHERE id = ? ORDER BY version DESC LIMIT 1) as latest_md5 " +
                    "FROM rule_class WHERE id = ? GROUP BY id")) {
                stmt.setString(1, ruleId);
                stmt.setString(2, ruleId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                        latestVersion = rs.getInt("latest_version");
                        existingMd5 = rs.getString("latest_md5");
                        if (rs.wasNull()) {
                            existingMd5 = "";
                        }
                    }
                }
            }
            
            // 检查MD5是否相同，如果相同则不需要创建新版本或更新
            if (exists && md5Hash != null && md5Hash.equals(existingMd5)) {
                LOGGER.info("规则 {} (版本 {}) 源代码MD5未变化 ({}), 无需更新", 
                        ruleId, latestVersion, md5Hash);
                return 2;  // 返回MD5相同跳过
            }
            
            // 判断是否需要创建新版本
            boolean needNewVersion = false;
            
            if (exists) {
                // 如果源代码变更或从无到有，创建新版本
                if (md5Hash != null && !md5Hash.equals(existingMd5)) {
                    needNewVersion = true;
                    LOGGER.info("规则 {} MD5变更，将创建新版本 {}", ruleId, latestVersion + 1);
                } else if (md5Hash == null && existingMd5 != null && !existingMd5.isEmpty()) {
                    needNewVersion = true;
                    LOGGER.info("规则 {} 源代码被移除，将创建新版本 {}", ruleId, latestVersion + 1);
                }
            }
            
            // 确定版本号
            int version = exists ? (needNewVersion ? latestVersion + 1 : latestVersion) : 1;
            
            // 插入新记录（无论是否已存在，都插入新记录）
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO rule_class (id, version, name, description, category, rule_code, priority, " +
                    "source_code, md5_hash, enabled_factories, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                stmt.setString(1, ruleId);
                stmt.setInt(2, version);
                stmt.setString(3, className);
                stmt.setString(4, annotation.description());
                stmt.setString(5, annotation.category().name());
                stmt.setInt(6, ruleCode);
                stmt.setInt(7, annotation.priority());
                
                // 设置源代码和MD5
                stmt.setString(8, sourceCode);
                stmt.setString(9, md5Hash);
                
                stmt.setString(10, exists ? getEnabledFactories(conn, ruleId) : "0"); // 保持已有的启用车厂设置
                Timestamp now = new Timestamp(new Date().getTime());
                stmt.setTimestamp(11, now);
                stmt.setTimestamp(12, now);
                
                int inserted = stmt.executeUpdate();
                
                if (inserted > 0) {
                    if (!exists) {
                        LOGGER.info("创建新规则 {} 版本 1", ruleId);
                    } else if (needNewVersion) {
                        LOGGER.info("创建规则 {} 新版本 {}", ruleId, version);
                    } else {
                        LOGGER.info("更新规则 {} 当前版本 {}", ruleId, version);
                    }
                    
                    // 创建规则信息并通知观察者
                    RuleInfo ruleInfo = new RuleInfo(ruleId, className, sourceCode, version, md5Hash);
                    updateManager.updateRule(ruleId, ruleInfo);
                    updateManager.notifyObservers();
                    
                    return 1; // 成功上传
                }
                
                return 0; // 失败
            }
        } catch (SQLException e) {
            LOGGER.error("数据库操作失败", e);
            return 0; // 失败
        }
    }
    
    /**
     * 获取规则的启用车厂列表
     * 
     * @param conn 数据库连接
     * @param ruleId 规则ID
     * @return 启用车厂列表
     * @throws SQLException 如果数据库操作失败
     */
    private String getEnabledFactories(Connection conn, String ruleId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT enabled_factories FROM rule_class WHERE id = ? ORDER BY version DESC LIMIT 1")) {
            stmt.setString(1, ruleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("enabled_factories");
                }
                return "0";
            }
        }
    }
    
    /**
     * 获取除指定车厂外的所有车厂列表
     * 
     * @param excludeFactoryId 要排除的车厂ID
     * @return 启用车厂列表
     */
    private String getAllFactoriesExcept(String excludeFactoryId) {
        StringBuilder sb = new StringBuilder(",");
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DISTINCT factory_id FROM vehicle_factory WHERE factory_id != ?")) {
                stmt.setString(1, excludeFactoryId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        sb.append(rs.getString("factory_id")).append(",");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("获取车厂列表失败", e);
        }
        
        return sb.toString();
    }
    
    /**
     * 读取类的源代码
     * 
     * @param clazz 类对象
     * @return 源代码字符串
     */
    private String readSourceCode(Class<?> clazz) {
        try {
            // 获取项目根目录
            String projectDir = System.getProperty("user.dir");
            
            // 从项目源码目录构造.java文件路径
            String relativePath = clazz.getName().replace('.', File.separatorChar) + ".java";
            Path sourcePath = Paths.get(projectDir, "src", "main", "java", relativePath);
            
            // 检查文件是否存在
            if (Files.exists(sourcePath)) {
                // 读取源文件内容
                return new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
            } else {
                LOGGER.warn("找不到源代码文件: {}", sourcePath);
                
                // 尝试从类路径读取
                String resourcePath = "/" + clazz.getName().replace('.', '/') + ".java";
                try (InputStream is = clazz.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        ByteArrayOutputStream result = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) != -1) {
                            result.write(buffer, 0, length);
                        }
                        return result.toString(StandardCharsets.UTF_8.name());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("读取源代码失败: {}", clazz.getName(), e);
        }
        
        return null;
    }
    
    /**
     * 计算字符串的MD5哈希值
     * 
     * @param input 输入字符串
     * @return MD5哈希值的十六进制字符串
     */
    private String calculateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("计算MD5失败", e);
            return null;
        }
    }
} 