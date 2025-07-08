package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 最小电压有效性检查规则
 */
@RuleDefinition(
    type = "MIN_VOLTAGE_VALIDITY",
    code = 1011,
    description = "最小电压无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class MinVoltageValidityRule extends AbstractRule {
    
    private static final int MIN_VOLTAGE = 0;
    private static final int MAX_VOLTAGE = 15000; // 单位 0.001V，15000表示15V

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer voltage = data.getBatteryMinVoltage();
        if (voltage == null) {
            return noIssue();
        }
        
        if (voltage < MIN_VOLTAGE || voltage > MAX_VOLTAGE) {
            return singleIssue(data, 
                    String.format("最小电压: %d (0.001V)", voltage));
        }
        
        return noIssue();
    }
} 
