package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 最大电压有效性检查规则
 */
@QualityRule(
    type = "MAX_VOLTAGE_VALIDITY",
    code = 1010,
    description = "最大电压无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class MaxVoltageValidityRule extends BaseRule {
    
    private static final int MIN_VOLTAGE = 0;
    private static final int MAX_VOLTAGE = 15000; // 单位 0.001V，15000表示15V

    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer voltage = data.getBatteryMaxVoltage();
        if (voltage == null) {
            return noIssue();
        }
        
        if (voltage < MIN_VOLTAGE || voltage > MAX_VOLTAGE) {
            return singleIssue(data, 
                    String.format("最大电压: %d (0.001V)", voltage));
        }
        
        return noIssue();
    }
} 
