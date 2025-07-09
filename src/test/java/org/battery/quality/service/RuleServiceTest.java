package org.battery.quality.service;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 规则服务测试
 * 验证三种变更场景：新增、修改、删除
 */
public class RuleServiceTest {
    
    @Mock
    private RuleEngine ruleEngine;
    
    private RuleService ruleService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ruleService = new RuleService();
    }
    
    @Test
    void testNewRuleAddition() {
        // 模拟新增规则场景
        Map<String, RuleInfo> newRules = new HashMap<>();
        
        RuleInfo newRule = createTestRule("NEW_RULE", "新规则", getCurrentTimestamp());
        newRules.put("NEW_RULE", newRule);
        
        // 模拟数据库返回
        when(ruleEngine.hasRule("NEW_RULE")).thenReturn(false);
        
        // 执行更新
        RuleUpdateResult result = ruleService.updateRules(ruleEngine);
        
        // 验证结果
        assertEquals(1, result.addedCount);
        assertEquals(0, result.modifiedCount);
        assertEquals(0, result.deletedCount);
    }
    
    @Test
    void testRuleModification() {
        // 模拟修改规则场景
        // 首先添加一个规则到本地快照
        RuleInfo oldRule = createTestRule("EXISTING_RULE", "旧规则", getOldTimestamp());
        // 模拟本地快照中已有此规则
        
        // 创建修改后的规则
        RuleInfo modifiedRule = createTestRule("EXISTING_RULE", "修改后的规则", getCurrentTimestamp());
        
        Map<String, RuleInfo> latestRules = new HashMap<>();
        latestRules.put("EXISTING_RULE", modifiedRule);
        
        when(ruleEngine.hasRule("EXISTING_RULE")).thenReturn(true);
        
        // 执行更新
        RuleUpdateResult result = ruleService.updateRules(ruleEngine);
        
        // 验证结果
        assertEquals(0, result.addedCount);
        assertEquals(1, result.modifiedCount);
        assertEquals(0, result.deletedCount);
        
        // 验证先删除后添加的操作
        verify(ruleEngine).removeRule("EXISTING_RULE");
        verify(ruleEngine).registerRule(any(), any());
    }
    
    @Test
    void testRuleDeletion() {
        // 模拟删除规则场景
        // 本地快照中有规则，但数据库中已删除
        
        // 模拟本地快照中有规则
        RuleInfo existingRule = createTestRule("TO_DELETE_RULE", "待删除规则", getCurrentTimestamp());
        
        // 数据库返回空（规则已删除）
        Map<String, RuleInfo> emptyRules = new HashMap<>();
        
        // 执行更新
        RuleUpdateResult result = ruleService.updateRules(ruleEngine);
        
        // 验证结果
        assertEquals(0, result.addedCount);
        assertEquals(0, result.modifiedCount);
        assertEquals(1, result.deletedCount);
        
        // 验证删除操作
        verify(ruleEngine).removeRule("TO_DELETE_RULE");
    }
    
    /**
     * 创建测试规则
     */
    private RuleInfo createTestRule(String id, String description, Timestamp updateTime) {
        return RuleInfo.builder()
                .id(id)
                .name(id + "Class")
                .description(description)
                .category("VALIDITY")
                .ruleCode(1001)
                .priority(5)
                .sourceCode(generateTestRuleCode(id))
                .enabledFactories("0")
                .updateTime(updateTime)
                .status(1)
                .build();
    }
    
    /**
     * 生成测试规则代码
     */
    private String generateTestRuleCode(String ruleId) {
        return String.format("""
                package org.battery.quality.rule.impl.test;
                
                import org.battery.quality.model.BatteryData;
                import org.battery.quality.model.QualityIssue;
                import org.battery.quality.rule.AbstractRule;
                import org.battery.quality.rule.RuleCategory;
                import org.battery.quality.rule.annotation.RuleDefinition;
                
                import java.util.List;
                
                @RuleDefinition(
                    type = "%s",
                    code = 1001,
                    description = "测试规则",
                    category = RuleCategory.VALIDITY
                )
                public class %sClass extends AbstractRule {
                    @Override
                    public List<QualityIssue> check(BatteryData data) {
                        return noIssue();
                    }
                }
                """, ruleId, ruleId);
    }
    
    private Timestamp getCurrentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
    
    private Timestamp getOldTimestamp() {
        return new Timestamp(System.currentTimeMillis() - 60000); // 1分钟前
    }
}
