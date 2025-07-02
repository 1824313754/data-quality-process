package org.battery.quality.rule;

import org.battery.quality.config.AppConfig;
import org.battery.quality.config.AppConfigLoader;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.annotation.QualityRule;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则管理器
 * 提供规则相关的所有操作，包括初始化、上传、查询等
 */
public class RuleManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleManager.class);
    
    /**
     * 上传所有规则源码（手动更新规则使用）
     * @return 上传成功的规则数量
     */
    public static int uploadAllRules() {
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
                    
                    // 如果有源代码，上传到数据库
                    if (sourceCode != null && !sourceCode.isEmpty()) {
                        // 上传到数据库
                        int result = saveRuleToDatabase(clazz.getName(), annotation, sourceCode);
                        if (result > 0) { // 成功上传
                            successCount++;
                            LOGGER.info("成功上传规则: ID={}, 类名={}", 
                                annotation.type(), clazz.getName());
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
    
    /**
     * 保存规则到数据库
     * @param className 类名
     * @param annotation 规则注解
     * @param sourceCode 源代码
     * @return 保存结果：0-失败，1-成功
     */
    private static int saveRuleToDatabase(String className, QualityRule annotation, 
            String sourceCode) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // 获取规则ID
            String ruleId = annotation.type();
            int ruleCode = annotation.code();
            LOGGER.debug("处理规则: ID={}, 类名={}, 异常编码={}", ruleId, className, ruleCode);
            
            // 检查规则是否已存在
            boolean exists = false;
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM rule_class WHERE id = ?")) {
                stmt.setString(1, ruleId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }
            
            // 准备SQL语句（插入或更新）
            String sql;
            if (exists) {
                // 更新现有规则
                sql = "UPDATE rule_class SET name = ?, description = ?, category = ?, rule_code = ?, " +
                      "priority = ?, source_code = ?, update_time = ? WHERE id = ?";
            } else {
                // 插入新规则
                sql = "INSERT INTO rule_class (name, description, category, rule_code, priority, " +
                      "source_code, enabled_factories, create_time, update_time, id) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                stmt.setString(idx++, className);
                stmt.setString(idx++, annotation.description());
                stmt.setString(idx++, annotation.category().name());
                stmt.setInt(idx++, ruleCode);
                stmt.setInt(idx++, annotation.priority());
                stmt.setString(idx++, sourceCode);
                
                Timestamp now = new Timestamp(new Date().getTime());
                
                if (exists) {
                    // 更新
                    stmt.setTimestamp(idx++, now); // update_time
                    stmt.setString(idx++, ruleId); // id (WHERE条件)
                } else {
                    // 插入
                    stmt.setString(idx++, "0"); // enabled_factories
                    stmt.setTimestamp(idx++, now); // create_time
                    stmt.setTimestamp(idx++, now); // update_time
                    stmt.setString(idx++, ruleId); // id
                }
                
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    if (exists) {
                        LOGGER.info("更新规则 {} 成功", ruleId);
                    } else {
                        LOGGER.info("创建新规则 {} 成功", ruleId);
                    }
                    return 1; // 成功
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
     * @param conn 数据库连接
     * @param ruleId 规则ID
     * @return 启用车厂列表
     * @throws SQLException 如果数据库操作失败
     */
    private static String getEnabledFactories(Connection conn, String ruleId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT enabled_factories FROM rule_class WHERE id = ?")) {
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
     * 读取类的源代码
     * @param clazz 类对象
     * @return 源代码字符串
     */
    private static String readSourceCode(Class<?> clazz) {
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
            }
        } catch (Exception e) {
            LOGGER.error("读取源代码失败: " + clazz.getName(), e);
        }
        return null;
    }
    
    // 规则实例缓存
    private static final Map<String, Rule> ruleInstanceCache = new ConcurrentHashMap<>();
    
    /**
     * 检查类是否实现了Rule接口
     */
    private static boolean implementsRuleInterface(Class<?> clazz) {
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            if (interfaceClass.equals(Rule.class)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 清除规则缓存
     */
    public static void clearRuleCache() {
        ruleInstanceCache.clear();
        LOGGER.info("规则缓存已清空");
    }
    
    /**
     * 加载所有规则
     * @return 规则映射（规则ID -> 规则对象）
     */
    public static Map<String, RuleInfo> loadAllRules() {
        Map<String, RuleInfo> rules = new HashMap<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, name, source_code, enabled_factories FROM rule_class")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String sourceCode = rs.getString("source_code");
                    String enabledFactories = rs.getString("enabled_factories");
                    
                    // 如果enabledFactories为null，默认设置为"0"（适用于所有车厂）
                    if (enabledFactories == null) {
                        enabledFactories = "0";
                    }
                    
                    RuleInfo ruleInfo = new RuleInfo(id, name, sourceCode, enabledFactories);
                    rules.put(id, ruleInfo);
                    LOGGER.debug("加载规则: ID={}, 类名={}, 适用车厂={}", id, name, enabledFactories);
                }
            }
            
            LOGGER.info("共加载 {} 个规则", rules.size());
        } catch (SQLException e) {
            LOGGER.error("从数据库加载规则失败", e);
        }
        
        return rules;
    }
    
    /**
     * 根据规则信息创建规则实例
     * @param ruleInfo 规则信息
     * @return 规则实例，如果创建失败则返回null
     */
    public static Rule createRuleInstance(RuleInfo ruleInfo) {
        if (ruleInfo == null) {
            return null;
        }
        
        try {
            // 动态编译规则类
            Class<?> ruleClass = DynamicCompiler.compile(ruleInfo.getName(), ruleInfo.getSourceCode());
            
            // 实例化规则
            Rule rule = (Rule) ruleClass.getDeclaredConstructor().newInstance();
            

            LOGGER.info("成功编译并加载规则: ID={}, 类名={}", 
                    ruleInfo.getId(), ruleInfo.getName());
            
            return rule;
        } catch (Exception e) {
            LOGGER.error("动态编译规则失败: ID={}, 类名={}", 
                    ruleInfo.getId(), ruleInfo.getName(), e);
            return null;
        }
    }
    
    public static void main(String[] args) {
        // 加载应用配置
        AppConfig appConfig = AppConfigLoader.load();

        // 初始化数据库连接
        DatabaseManager.getInstance().initDataSource(appConfig.getMysql());
        uploadAllRules();
    }
} 