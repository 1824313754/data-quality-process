package org.battery.quality.dao;

import org.battery.quality.config.AppConfig;
import org.battery.quality.config.ConfigManager;
import org.battery.quality.config.DatabaseManager;
import org.battery.quality.model.RuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 规则数据访问对象
 * 负责从数据库加载规则信息
 */
public class RuleDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleDao.class);
    
    // 数据库管理器
    private final DatabaseManager dbManager;
    
    // 查询所有规则的SQL
    private static final String SQL_LOAD_ALL_RULES = 
            "SELECT id, name, description, category, rule_code, priority, " +
            "source_code, enabled_factories, create_time, update_time, status " +
            "FROM rule_class WHERE status = 1";
    
    /**
     * 构造函数
     */
    public RuleDao() {
        dbManager = DatabaseManager.getInstance();
        
        // 初始化Doris数据库连接池
        AppConfig appConfig = ConfigManager.getInstance().getConfig();
        dbManager.initDataSource(appConfig.getDorisRule());
    }
    
    /**
     * 加载所有启用的规则
     * 
     * @return 规则映射，键为规则ID，值为规则信息
     */
    public Map<String, RuleInfo> loadAllRules() {
        Map<String, RuleInfo> ruleMap = new HashMap<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LOAD_ALL_RULES);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                String category = rs.getString("category");
                int ruleCode = rs.getInt("rule_code");
                int priority = rs.getInt("priority");
                String sourceCode = rs.getString("source_code");
                String enabledFactories = rs.getString("enabled_factories");
                int status = rs.getInt("status");
                
                RuleInfo rule = RuleInfo.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .category(category)
                    .ruleCode(ruleCode)
                    .priority(priority)
                    .sourceCode(sourceCode)
                    .enabledFactories(enabledFactories)
                    .createTime(rs.getTimestamp("create_time"))
                    .updateTime(rs.getTimestamp("update_time"))
                    .status(status)
                    .build();
                
                ruleMap.put(rule.getId(), rule);
            }
            
            LOGGER.info("从数据库加载了 {} 条规则", ruleMap.size());
        } catch (SQLException e) {
            LOGGER.error("加载规则失败", e);
        }
        
        return ruleMap;
    }
} 