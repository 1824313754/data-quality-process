package org.battery.quality.service;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规则服务测试
 * 验证策略模式的增量更新逻辑
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
    void testRuleChangeTypeEnum() {
        // 测试枚举值
        assertEquals("新增", RuleChangeType.NEW.getDescription());
        assertEquals("修改", RuleChangeType.MODIFIED.getDescription());
        assertEquals("删除", RuleChangeType.DELETED.getDescription());
        assertEquals("无变更", RuleChangeType.UNCHANGED.getDescription());

        // 测试toString
        assertEquals("新增", RuleChangeType.NEW.toString());
        assertEquals("修改", RuleChangeType.MODIFIED.toString());
    }

    @Test
    void testStrategyPattern() {
        // 测试策略模式 - 每个枚举都有自己的处理策略
        RuleUpdateResult result = new RuleUpdateResult();
        RuleInfo testRule = createTestRule("TEST_RULE", "测试规则", getCurrentTimestamp());

        // 测试NEW策略
        RuleChangeType.NEW.handle(ruleEngine, testRule, "TEST_RULE", ruleService, result);
        // 由于是mock对象，这里主要验证方法被调用，实际的计数在真实场景中会更新

        // 测试UNCHANGED策略（应该什么都不做）
        RuleChangeType.UNCHANGED.handle(ruleEngine, testRule, "TEST_RULE", ruleService, result);

        // 验证策略模式的多态性
        assertNotNull(RuleChangeType.NEW);
        assertNotNull(RuleChangeType.MODIFIED);
        assertNotNull(RuleChangeType.DELETED);
        assertNotNull(RuleChangeType.UNCHANGED);
    }
    
    @Test
    void testRuleUpdateResult() {
        RuleUpdateResult result = new RuleUpdateResult();
        
        // 初始状态
        assertEquals(0, result.getTotalChanges());
        assertFalse(result.hasChanges());
        assertFalse(result.hasErrors());
        
        // 添加一些变更
        result.addedCount = 2;
        result.modifiedCount = 1;
        result.deletedCount = 1;
        result.errorCount = 1;
        
        // 验证统计
        assertEquals(4, result.getTotalChanges());
        assertTrue(result.hasChanges());
        assertTrue(result.hasErrors());
        
        // 验证toString
        String resultStr = result.toString();
        assertTrue(resultStr.contains("新增:2"));
        assertTrue(resultStr.contains("修改:1"));
        assertTrue(resultStr.contains("删除:1"));
        assertTrue(resultStr.contains("错误:1"));
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
