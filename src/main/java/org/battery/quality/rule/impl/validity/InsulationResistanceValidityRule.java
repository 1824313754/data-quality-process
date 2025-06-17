package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 绝缘阻值有效性检查规则
 */
@QualityRule(
    type = "INSULATION_RESISTANCE_VALIDITY",
    code = 1009,
    description = "绝缘阻值无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class InsulationResistanceValidityRule extends BaseRule {
    
    private static final int MIN_RESISTANCE = 200;   // 最小值200
    private static final int MAX_RESISTANCE = 60000; // 最大值60000

    @Override
    public List<Issue> check(Gb32960Data data) {
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
