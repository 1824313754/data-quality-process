package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 最高单体电压有效性检查规则
 */
@QualityRule(
    type = "MAX_VOLTAGE_VALIDITY",
    code = 1004,
    description = "最高单体电压无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class MaxVoltageValidityRule extends AbstractRule {
    
    private static final int MIN_VOLTAGE = 0;
    private static final int MAX_VOLTAGE = 5000; // 单位 mV

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer voltage = data.getBatteryMaxVoltage();
        if (voltage == null) {
            return noIssue();
        }
        
        if (voltage < MIN_VOLTAGE || voltage > MAX_VOLTAGE) {
            return singleIssue(data, 
                    String.format("最高单体电压: %d mV", voltage));
        }
        
        return noIssue();
    }
} 
