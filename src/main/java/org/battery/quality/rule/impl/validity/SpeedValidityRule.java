package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 车速有效性检查规则
 */
@RuleDefinition(
    type = "SPEED_VALIDITY",
    code = 1006,
    description = "车速无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class SpeedValidityRule extends AbstractRule {
    
    private static final int MIN_SPEED = 0;
    private static final int MAX_SPEED = 2200; // 单位 0.1km/h，2200表示220km/h

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer speed = data.getSpeed();
        if (speed == null) {
            return noIssue();
        }
        
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            return singleIssue(data, 
                    String.format("车速: %d (0.1km/h)", speed));
        }
        
        return noIssue();
    }
} 
