package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 总电压有效性检查规则
 */
@QualityRule(
    type = "TOTAL_VOLTAGE_VALIDITY",
    code = 1003,
    description = "总电压无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class TotalVoltageValidityRule extends BaseRule {
    
    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer voltage = data.getTotalVoltage();
        if (voltage == null) {
            return noIssue();
        }
        
        // 总电压取值范围: [0, 10000] (单位: 0.1V)
        if (voltage < 0 || voltage > 10000) {
            return singleIssue(data, 
                    String.format("总电压: %.1fV", voltage / 10.0));
        }
        
        return noIssue();
    }
} 
