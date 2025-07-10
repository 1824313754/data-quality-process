package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 总电压有效性检查规则
 */
@RuleDefinition(
    type = "TOTAL_VOLTAGE_VALIDITY",
    code = 1003,
    description = "总电压无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class TotalVoltageValidityRule extends AbstractRule {
    
    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer voltage = data.getTotalVoltage();
        if (voltage == null) {
            return noIssue();
        }

        // 总电压取值范围: [0, 1000V] 根据指标表修正
        if (voltage < 0 || voltage > 10000) {
            return singleIssue(data,
                    String.format("总电压: %dV", voltage));
        }

        return noIssue();
    }
} 
