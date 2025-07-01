package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 里程有效性检查规则
 */
@QualityRule(
    type = "MILEAGE_VALIDITY",
    code = 1005,
    description = "里程无效",
    category = RuleType.VALIDITY,
    priority = 3
)
public class MileageValidityRule extends AbstractRule {
    
    private static final int MIN_MILEAGE = 0;
    private static final int MAX_MILEAGE = 2000000; // 单位 km

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer mileage = data.getMileage();
        if (mileage == null) {
            return noIssue();
        }
        
        if (mileage < MIN_MILEAGE || mileage > MAX_MILEAGE) {
            return singleIssue(data, 
                    String.format("里程值: %d km", mileage));
        }
        
        return noIssue();
    }
} 
