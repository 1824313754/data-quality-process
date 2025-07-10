package org.battery.quality.service.strategy;

import org.battery.quality.model.RuleInfo;
import org.battery.quality.rule.IRule;
import org.battery.quality.rule.RuleEngine;
import org.battery.quality.service.RuleService;
import org.battery.quality.service.RuleUpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 规则变更策略测试
 */
public class RuleChangeStrategyTest {
    
    @Mock
    private RuleEngine ruleEngine;
    
    @Mock
    private RuleService ruleService;
    
    @Mock
    private IRule mockRule;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testNewRuleStrategy() {
        // 准备测试数据
        NewRuleStrategy strategy = new NewRuleStrategy();
        RuleInfo ruleInfo = createTestRule("NEW_RULE", "新规则");
        RuleUpdateResult result = new RuleUpdateResult();
        
        // 模拟服务调用
        when(ruleService.createRule(ruleInfo)).thenReturn(mockRule);
        when(ruleService.parseFactories(anyString())).thenReturn(Arrays.asList("0"));
        
        // 执行策略
        strategy.handle(ruleEngine, ruleInfo, "NEW_RULE", ruleService, result);
        
        // 验证结果
        assertEquals(1, result.addedCount);
        assertEquals(0, result.errorCount);
        
        // 验证调用
        verify(ruleService).createRule(ruleInfo);
        verify(ruleService).parseFactories(ruleInfo.getEnabledFactories());
        verify(ruleEngine).registerRule(mockRule, Arrays.asList("0"));
        verify(ruleService).updateLocalSnapshot("NEW_RULE", ruleInfo);
    }
    
    @Test
    void testModifiedRuleStrategy() {
        // 准备测试数据
        ModifiedRuleStrategy strategy = new ModifiedRuleStrategy();
        RuleInfo ruleInfo = createTestRule("MODIFIED_RULE", "修改后的规则");
        RuleUpdateResult result = new RuleUpdateResult();
        
        // 模拟服务调用
        when(ruleService.createRule(ruleInfo)).thenReturn(mockRule);
        when(ruleService.parseFactories(anyString())).thenReturn(Arrays.asList("0"));
        
        // 执行策略
        strategy.handle(ruleEngine, ruleInfo, "MODIFIED_RULE", ruleService, result);
        
        // 验证结果
        assertEquals(1, result.modifiedCount);
        assertEquals(0, result.errorCount);
        
        // 验证调用顺序：先删除后添加
        verify(ruleEngine).removeRule("MODIFIED_RULE");
        verify(ruleService).createRule(ruleInfo);
        verify(ruleEngine).registerRule(mockRule, Arrays.asList("0"));
        verify(ruleService).updateLocalSnapshot("MODIFIED_RULE", ruleInfo);
    }
    
    @Test
    void testDeletedRuleStrategy() {
        // 准备测试数据
        DeletedRuleStrategy strategy = new DeletedRuleStrategy();
        RuleUpdateResult result = new RuleUpdateResult();
        
        // 执行策略
        strategy.handle(ruleEngine, null, "DELETED_RULE", ruleService, result);
        
        // 验证结果
        assertEquals(1, result.deletedCount);
        assertEquals(0, result.errorCount);
        
        // 验证调用
        verify(ruleEngine).removeRule("DELETED_RULE");
        verify(ruleService).removeFromLocalSnapshot("DELETED_RULE");
    }
    
    @Test
    void testUnchangedRuleStrategy() {
        // 准备测试数据
        UnchangedRuleStrategy strategy = new UnchangedRuleStrategy();
        RuleInfo ruleInfo = createTestRule("UNCHANGED_RULE", "无变更规则");
        RuleUpdateResult result = new RuleUpdateResult();
        
        // 执行策略
        strategy.handle(ruleEngine, ruleInfo, "UNCHANGED_RULE", ruleService, result);
        
        // 验证结果 - 应该没有任何变更
        assertEquals(0, result.addedCount);
        assertEquals(0, result.modifiedCount);
        assertEquals(0, result.deletedCount);
        assertEquals(0, result.errorCount);
        
        // 验证没有任何操作
        verifyNoInteractions(ruleEngine);
        verifyNoInteractions(ruleService);
    }
    
    @Test
    void testNewRuleStrategyWithError() {
        // 测试编译失败的情况
        NewRuleStrategy strategy = new NewRuleStrategy();
        RuleInfo ruleInfo = createTestRule("ERROR_RULE", "错误规则");
        RuleUpdateResult result = new RuleUpdateResult();
        
        // 模拟编译失败
        when(ruleService.createRule(ruleInfo)).thenReturn(null);
        
        // 执行策略
        strategy.handle(ruleEngine, ruleInfo, "ERROR_RULE", ruleService, result);
        
        // 验证结果
        assertEquals(0, result.addedCount);
        assertEquals(1, result.errorCount);
        
        // 验证只调用了编译，没有注册
        verify(ruleService).createRule(ruleInfo);
        verify(ruleEngine, never()).registerRule(any(), any());
    }
    
    /**
     * 创建测试规则
     */
    private RuleInfo createTestRule(String id, String description) {
        return RuleInfo.builder()
                .id(id)
                .name(id + "Class")
                .description(description)
                .category("VALIDITY")
                .ruleCode(1001)
                .priority(5)
                .sourceCode("test source code")
                .enabledFactories("0")
                .updateTime(new Timestamp(System.currentTimeMillis()))
                .status(1)
                .build();
    }
}
