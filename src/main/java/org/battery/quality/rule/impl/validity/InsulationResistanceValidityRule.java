package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 绝缘阻值有效性检查规则
 */
@RuleDefinition(
    type = "INSULATION_RESISTANCE_VALIDITY",
    code = 1009,
    description = "绝缘阻值无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class InsulationResistanceValidityRule extends AbstractRule {
    
    private static final int MIN_RESISTANCE = 0;   // 最小值200
    private static final int MAX_RESISTANCE = 60000; // 最大值60000

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer resistance = data.getInsulationResistance();
        if (resistance == null) {
            return noIssue();
        }
        
        if (resistance < MIN_RESISTANCE || resistance > MAX_RESISTANCE) {
            return singleIssue(data, 
                    String.format("绝缘阻值: %d", resistance));
        }
        
        return noIssue();
    }
} 
