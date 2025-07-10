package org.battery.quality.util;

import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.rule.RuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则上传工具
 * 用于将规则源代码上传到MySQL数据库
 */
public class RuleUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleUploader.class);
    
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
        System.out.println("=== 规则上传工具 ===");
        
        // 初始化数据库连接
        initDatabase();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n请选择操作：");
            System.out.println("1. 上传单个规则文件");
            System.out.println("2. 批量上传目录下的规则");
            System.out.println("3. 退出");
            System.out.print("请输入选项（1-3）：");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    uploadSingleRule(scanner);
                    break;
                case "2":
                    uploadRulesFromDirectory(scanner);
                    break;
                case "3":
                    System.out.println("感谢使用，再见！");
                    return;
                default:
                    System.out.println("无效的选项，请重新输入。");
            }
        }
    }
    
    /**
     * 初始化数据库连接
     */
    private static void initDatabase() {
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
    }
    
    /**
     * 上传单个规则文件
     */
    private static void uploadSingleRule(Scanner scanner) {
        System.out.print("\n请输入规则文件路径：");
        String filePath = scanner.nextLine().trim();
        
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("文件不存在或不是一个有效的文件！");
            return;
        }
        
        try {
            String sourceCode = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            
            // 解析规则信息
            RuleInfo ruleInfo = parseRuleInfo(sourceCode, file.getName());
            
            // 输入适用的车厂
            System.out.print("请输入适用的车厂ID（多个ID用逗号分隔，输入0表示所有车厂）：");
            String enabledFactories = scanner.nextLine().trim();
            if (enabledFactories.isEmpty()) {
                enabledFactories = "0";
            }
            
            // 确认上传
            System.out.println("\n规则信息：");
            System.out.println("ID: " + ruleInfo.id);
            System.out.println("名称: " + ruleInfo.name);
            System.out.println("描述: " + ruleInfo.description);
            System.out.println("分类: " + ruleInfo.category);
            System.out.println("异常编码: " + ruleInfo.ruleCode);
            System.out.println("优先级: " + ruleInfo.priority);
            System.out.println("适用车厂: " + enabledFactories);
            
            System.out.print("\n确认上传？(y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if (confirm.equals("y")) {
                // 上传规则
                uploadRule(ruleInfo, enabledFactories, sourceCode);
                System.out.println("规则上传成功！");
            } else {
                System.out.println("已取消上传。");
            }
        } catch (Exception e) {
            System.err.println("上传规则失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量上传目录下的规则
     */
    private static void uploadRulesFromDirectory(Scanner scanner) {
        System.out.print("\n请输入规则目录路径：");
        String dirPath = scanner.nextLine().trim();
        
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("目录不存在或不是一个有效的目录！");
            return;
        }
        
        System.out.print("请输入适用的车厂ID（多个ID用逗号分隔，输入0表示所有车厂）：");
        String enabledFactories = scanner.nextLine().trim();
        if (enabledFactories.isEmpty()) {
            enabledFactories = "0";
        }
        
        try {
            // 递归查找所有Java文件
            List<File> javaFiles = findJavaFilesRecursively(dir);
            if (javaFiles.isEmpty()) {
                System.out.println("目录中没有找到Java文件。");
                return;
            }

            System.out.println("\n递归查找到 " + javaFiles.size() + " 个Java文件，准备上传...");

            int successCount = 0;
            for (File file : javaFiles) {
                try {
                    String sourceCode = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    RuleInfo ruleInfo = parseRuleInfo(sourceCode, file.getName());

                    // 上传规则
                    uploadRule(ruleInfo, enabledFactories, sourceCode);
                    System.out.println("上传成功: " + getRelativePath(dir, file));
                    successCount++;
                } catch (Exception e) {
                    System.err.println("上传失败: " + getRelativePath(dir, file) + " - " + e.getMessage());
                }
            }

            System.out.println("\n批量上传完成！成功：" + successCount + "，失败：" + (javaFiles.size() - successCount));
        } catch (Exception e) {
            System.err.println("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 递归查找目录下所有Java文件
     * @param dir 根目录
     * @return Java文件列表
     */
    private static List<File> findJavaFilesRecursively(File dir) {
        List<File> javaFiles = new ArrayList<>();
        findJavaFilesRecursively(dir, javaFiles);
        return javaFiles;
    }

    /**
     * 递归查找Java文件的内部实现
     * @param dir 当前目录
     * @param javaFiles 结果列表
     */
    private static void findJavaFilesRecursively(File dir, List<File> javaFiles) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归查找子目录
                findJavaFilesRecursively(file, javaFiles);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                // 添加Java文件
                javaFiles.add(file);
            }
        }
    }

    /**
     * 获取文件相对于根目录的路径
     * @param rootDir 根目录
     * @param file 文件
     * @return 相对路径
     */
    private static String getRelativePath(File rootDir, File file) {
        try {
            Path rootPath = rootDir.toPath().toAbsolutePath();
            Path filePath = file.toPath().toAbsolutePath();
            return rootPath.relativize(filePath).toString();
        } catch (Exception e) {
            return file.getName();
        }
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