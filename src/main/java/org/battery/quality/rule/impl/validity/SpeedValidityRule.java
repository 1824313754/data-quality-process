package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 车速有效性检查规则
 */
@QualityRule(
    type = "SPEED_VALIDITY",
    code = 1010,
    description = "车速无效",
    category = RuleType.VALIDITY,
    priority = 4
)
public class SpeedValidityRule extends AbstractRule {
    
    private static final int MAX_SPEED = 300; // 最大车速 km/h

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer speed = data.getSpeed();
        if (speed == null) {
            return noIssue();
        }
        
        if (speed < 0 || speed > MAX_SPEED) {
            return singleIssue(data, 
                    String.format("车速值: %d km/h", speed));
        }
        
        return noIssue();
    }
} 
