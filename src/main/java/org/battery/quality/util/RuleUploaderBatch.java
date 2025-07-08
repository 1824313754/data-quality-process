package org.battery.quality.util;

import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.rule.RuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则批量上传工具
 * 通过命令行参数直接上传规则到MySQL数据库
 * 
 * 用法：
 * java -cp data-quality-process.jar org.battery.quality.util.RuleUploaderBatch [文件路径/目录路径] [车厂ID]
 */
public class RuleUploaderBatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleUploaderBatch.class);
    
    // 数据库管理器
    private static final DatabaseManager dbManager = DatabaseManager.getInstance();
    
    // 插入规则的SQL
    private static final String SQL_INSERT_RULE = 
            "INSERT INTO rule_class (id, name, description, category, rule_code, priority, " +
            "source_code, enabled_factories, create_time, update_time, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    // 更新规则的SQL
    private static final String SQL_UPDATE_RULE = 
            "UPDATE rule_class SET name = ?, description = ?, category = ?, rule_code = ?, " +
            "priority = ?, source_code = ?, enabled_factories = ?, update_time = ? " +
            "WHERE id = ?";
    
    // 查询规则的SQL
    private static final String SQL_QUERY_RULE = 
            "SELECT id FROM rule_class WHERE id = ?";
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java -cp data-quality-process.jar org.battery.quality.util.RuleUploaderBatch [文件路径/目录路径] [车厂ID]");
            System.out.println("示例: java -cp data-quality-process.jar org.battery.quality.util.RuleUploaderBatch src/main/java/org/battery/quality/rule/impl/validity 0");
            System.exit(1);
        }
        
        String path = args[0];
        String enabledFactories = args.length > 1 ? args[1] : "0";
        
        System.out.println("=== 规则批量上传工具 ===");
        System.out.println("路径: " + path);
        System.out.println("适用车厂: " + enabledFactories);
        
        // 初始化数据库连接
        try {
            // 加载配置
            AppConfig appConfig = ConfigManager.getInstance().getConfig();
            
            // 初始化数据库连接池
            dbManager.initDataSource(appConfig.getMysql());
            
            System.out.println("数据库连接初始化成功！");
        } catch (Exception e) {
            System.err.println("数据库连接初始化失败：" + e.getMessage());
            System.exit(1);
        }
        
        File file = new File(path);
        if (file.isFile()) {
            // 上传单个文件
            try {
                uploadSingleRule(file, enabledFactories);
            } catch (Exception e) {
                System.err.println("上传文件失败: " + e.getMessage());
            }
        } else if (file.isDirectory()) {
            // 上传目录下的所有Java文件
            try {
                uploadDirectory(file, enabledFactories);
            } catch (Exception e) {
                System.err.println("上传目录失败: " + e.getMessage());
            }
        } else {
            System.err.println("指定的路径不存在: " + path);
        }
    }
    
    /**
     * 上传单个规则文件
     */
    private static void uploadSingleRule(File file, String enabledFactories) throws Exception {
        if (!file.getName().endsWith(".java")) {
            System.out.println("跳过非Java文件: " + file.getName());
            return;
        }
        
        System.out.println("处理文件: " + file.getName());
        
        String sourceCode = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        RuleInfo ruleInfo = parseRuleInfo(sourceCode, file.getName());
        
        uploadRule(ruleInfo, enabledFactories, sourceCode);
        System.out.println("上传成功: " + file.getName());
    }
    
    /**
     * 上传目录下的所有Java文件
     */
    private static void uploadDirectory(File dir, String enabledFactories) throws Exception {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("目录为空: " + dir.getPath());
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                uploadDirectory(file, enabledFactories);
            } else if (file.getName().endsWith(".java")) {
                try {
                    uploadSingleRule(file, enabledFactories);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("上传失败: " + file.getName() + " - " + e.getMessage());
                    failCount++;
                }
            }
        }
        
        System.out.println("目录 " + dir.getPath() + " 处理完成，成功: " + successCount + ", 失败: " + failCount);
    }
    
    /**
     * 解析规则信息
     */
    private static RuleInfo parseRuleInfo(String sourceCode, String fileName) throws Exception {
        RuleInfo info = new RuleInfo();
        
        // 提取规则类型
        Pattern typePattern = Pattern.compile("@RuleDefinition\\s*\\([^)]*type\\s*=\\s*\"([^\"]+)\"", Pattern.DOTALL);
        Matcher typeMatcher = typePattern.matcher(sourceCode);
        if (typeMatcher.find()) {
            info.id = typeMatcher.group(1);
        } else {
            // 使用文件名作为ID
            info.id = fileName.replace(".java", "");
        }
        
        // 提取规则名称
        Pattern namePattern = Pattern.compile("class\\s+(\\w+)\\s+extends");
        Matcher nameMatcher = namePattern.matcher(sourceCode);
        if (nameMatcher.find()) {
            info.name = nameMatcher.group(1);
        } else {
            info.name = info.id;
        }
        
        // 提取规则描述
        Pattern descPattern = Pattern.compile("@RuleDefinition\\s*\\([^)]*description\\s*=\\s*\"([^\"]+)\"", Pattern.DOTALL);
        Matcher descMatcher = descPattern.matcher(sourceCode);
        if (descMatcher.find()) {
            info.description = descMatcher.group(1);
        } else {
            info.description = info.name;
        }
        
        // 提取规则分类
        Pattern categoryPattern = Pattern.compile("@RuleDefinition\\s*\\([^)]*category\\s*=\\s*RuleCategory\\.([^,\\)]+)", Pattern.DOTALL);
        Matcher categoryMatcher = categoryPattern.matcher(sourceCode);
        if (categoryMatcher.find()) {
            info.category = categoryMatcher.group(1);
        } else {
            // 从文件路径推断分类
            if (fileName.toLowerCase().contains("validity")) {
                info.category = RuleCategory.VALIDITY.name();
            } else if (fileName.toLowerCase().contains("consistency")) {
                info.category = RuleCategory.CONSISTENCY.name();
            } else if (fileName.toLowerCase().contains("timeliness")) {
                info.category = RuleCategory.TIMELINESS.name();
            } else if (fileName.toLowerCase().contains("completeness")) {
                info.category = RuleCategory.COMPLETENESS.name();
            } else {
                info.category = RuleCategory.VALIDITY.name();
            }
        }
        
        // 提取规则编码
        Pattern codePattern = Pattern.compile("@RuleDefinition\\s*\\([^)]*code\\s*=\\s*(\\d+)", Pattern.DOTALL);
        Matcher codeMatcher = codePattern.matcher(sourceCode);
        if (codeMatcher.find()) {
            info.ruleCode = Integer.parseInt(codeMatcher.group(1));
        } else {
            info.ruleCode = 1000; // 默认编码
        }
        
        // 提取优先级
        Pattern priorityPattern = Pattern.compile("@RuleDefinition\\s*\\([^)]*priority\\s*=\\s*(\\d+)", Pattern.DOTALL);
        Matcher priorityMatcher = priorityPattern.matcher(sourceCode);
        if (priorityMatcher.find()) {
            info.priority = Integer.parseInt(priorityMatcher.group(1));
        } else {
            info.priority = 5; // 默认优先级
        }
        
        return info;
    }
    
    /**
     * 上传规则到数据库
     */
    private static void uploadRule(RuleInfo ruleInfo, String enabledFactories, String sourceCode) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbManager.getConnection();
            
            // 检查规则是否已存在
            stmt = conn.prepareStatement(SQL_QUERY_RULE);
            stmt.setString(1, ruleInfo.id);
            rs = stmt.executeQuery();
            
            boolean exists = rs.next();
            rs.close();
            stmt.close();
            
            LocalDateTime now = LocalDateTime.now();
            String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            if (exists) {
                // 更新规则
                stmt = conn.prepareStatement(SQL_UPDATE_RULE);
                stmt.setString(1, ruleInfo.name);
                stmt.setString(2, ruleInfo.description);
                stmt.setString(3, ruleInfo.category);
                stmt.setInt(4, ruleInfo.ruleCode);
                stmt.setInt(5, ruleInfo.priority);
                stmt.setString(6, sourceCode);
                stmt.setString(7, enabledFactories);
                stmt.setString(8, currentTime);
                stmt.setString(9, ruleInfo.id);
                
                System.out.println("更新规则: " + ruleInfo.id);
            } else {
                // 插入新规则
                stmt = conn.prepareStatement(SQL_INSERT_RULE);
                stmt.setString(1, ruleInfo.id);
                stmt.setString(2, ruleInfo.name);
                stmt.setString(3, ruleInfo.description);
                stmt.setString(4, ruleInfo.category);
                stmt.setInt(5, ruleInfo.ruleCode);
                stmt.setInt(6, ruleInfo.priority);
                stmt.setString(7, sourceCode);
                stmt.setString(8, enabledFactories);
                stmt.setString(9, currentTime);
                stmt.setString(10, currentTime);
                stmt.setInt(11, 1); // 默认启用
                
                System.out.println("新增规则: " + ruleInfo.id);
            }
            
            stmt.executeUpdate();
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { /* ignore */ }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { /* ignore */ }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
    }
    
    /**
     * 规则信息类（内部使用）
     */
    private static class RuleInfo {
        String id;
        String name;
        String description;
        String category;
        int ruleCode;
        int priority;
    }
} 