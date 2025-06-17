package org.battery.quality.rule;

import org.battery.quality.config.DatabaseManager;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    
    /**
     * 保存规则到数据库
     * @param className 类名
     * @param annotation 规则注解
     * @param sourceCode 源代码
     * @param md5Hash MD5哈希值
     * @return 保存结果：0-失败，1-成功上传，2-MD5相同跳过
     */
    private static int saveRuleToDatabase(String className, QualityRule annotation, 
            String sourceCode, String md5Hash) {
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
     * @param conn 数据库连接
     * @param ruleId 规则ID
     * @return 启用车厂列表
     * @throws SQLException 如果数据库操作失败
     */
    private static String getEnabledFactories(Connection conn, String ruleId) throws SQLException {
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
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("读取源代码失败: " + clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 计算字符串的MD5哈希值
     * @param input 输入字符串
     * @return MD5哈希值的十六进制字符串
     */
    private static String calculateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("计算MD5失败", e);
            return null;
        }
    }
    
    // 缓存已加载的规则类实例，key为"规则ID:版本号"
    private static final Map<String, Rule> ruleInstanceCache = new ConcurrentHashMap<>();
    

    /**
     * 检查类是否实现了Rule接口
     * @param clazz 要检查的类
     * @return 是否实现了Rule接口
     */
    private static boolean implementsRuleInterface(Class<?> clazz) {
        // 获取类实现的所有接口
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getName().equals(Rule.class.getName())) {
                return true;
            }
        }

        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return implementsRuleInterface(superClass);
        }

        return false;
    }
    
    /**
     * 清除规则缓存
     * 在规则更新后调用此方法，确保下次加载时使用最新版本
     */
    public static void clearRuleCache() {
        ruleInstanceCache.clear();
        LOGGER.info("规则缓存已清除");
    }
    
    /**
     * 入口方法，用于直接执行上传工具
     */
    public static void main(String[] args) {
        try {
            // 初始化数据库连接
            DatabaseManager.getInstance().initDataSource(org.battery.quality.config.AppConfigLoader.load().getMysql());
            
            // 上传规则
            int successCount = uploadAllRules();
            
            System.out.println("规则上传完成，共上传成功 " + successCount + " 个规则");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            DatabaseManager.getInstance().closeDataSource();
        }
    }
} 