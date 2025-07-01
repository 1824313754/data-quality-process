package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 总电流有效性检查规则
 */
@QualityRule(
    type = "TOTAL_CURRENT_VALIDITY",
    code = 1011,
    description = "总电流无效",
    category = RuleType.VALIDITY,
    priority = 4
)
public class TotalCurrentValidityRule extends AbstractRule {
    
    private static final int MIN_CURRENT = -1000;  // 最小电流 -1000A
    private static final int MAX_CURRENT = 1000;   // 最大电流 1000A

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer current = data.getTotalCurrent();
        if (current == null) {
            return noIssue();
        }
        
        // 电流值已经减去10000的偏移量
        if (current < MIN_CURRENT || current > MAX_CURRENT) {
            return singleIssue(data, 
                    String.format("总电流: %d A", current));
        }
        
        return noIssue();
    }
} 
