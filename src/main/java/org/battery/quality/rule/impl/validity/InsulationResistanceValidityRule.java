package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 绝缘电阻有效性检查规则
 */
@QualityRule(
    type = "INSULATION_RESISTANCE_VALIDITY",
    code = 1003,
    description = "绝缘电阻无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class InsulationResistanceValidityRule extends AbstractRule {
    
    private static final int MIN_RESISTANCE = 0;
    private static final int MAX_RESISTANCE = 10000; // 10000 kΩ
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer resistance = data.getInsulationResistance();
        if (resistance == null) {
            return noIssue();
        }
        
        if (resistance < MIN_RESISTANCE || resistance > MAX_RESISTANCE) {
            return singleIssue(data, 
                    String.format("绝缘电阻: %d kΩ", resistance));
        }
        
        return noIssue();
    }
} 
