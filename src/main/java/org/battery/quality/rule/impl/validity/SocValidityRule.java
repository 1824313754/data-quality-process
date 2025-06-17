package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * SOC有效性检查规则
 */
@QualityRule(
    type = "SOC_VALIDITY",
    code = 1002,
    description = "SOC无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class SocValidityRule extends BaseRule {
    
    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer soc = data.getSoc();
        if (soc == null) {
            return noIssue();
        }
        
        // SOC取值范围: [0, 100]
        if (soc < 0 || soc > 100) {
            return singleIssue(data, 
                    String.format("SOC: %d", soc));
        }
        
        return noIssue();
    }
} 
