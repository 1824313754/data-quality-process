package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 里程有效性检查规则
 */
@RuleDefinition(
    type = "MILEAGE_VALIDITY",
    code = 1007,
    description = "里程无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class MileageValidityRule extends AbstractRule {
    
    private static final int MIN_MILEAGE = 0;
    private static final int MAX_MILEAGE = 9999999; // 最大里程

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer mileage = data.getMileage();
        if (mileage == null) {
            return noIssue();
        }
        
        if (mileage < MIN_MILEAGE || mileage > MAX_MILEAGE) {
            return singleIssue(data, 
                    String.format("里程: %d", mileage));
        }
        
        return noIssue();
    }
} 
