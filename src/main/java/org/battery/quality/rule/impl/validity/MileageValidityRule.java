package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 里程有效性检查规则
 */
@QualityRule(
    type = "MILEAGE_VALIDITY",
    code = 1007,
    description = "里程无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class MileageValidityRule extends BaseRule {
    
    private static final int MIN_MILEAGE = 0;
    private static final int MAX_MILEAGE = 9999999; // 最大里程

    @Override
    public List<Issue> check(Gb32960Data data) {
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
