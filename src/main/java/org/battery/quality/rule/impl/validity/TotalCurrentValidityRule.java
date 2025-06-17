package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 总电流有效性检查规则
 */
@QualityRule(
    type = "TOTAL_CURRENT_VALIDITY",
    code = 1004,
    description = "总电流无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class TotalCurrentValidityRule extends BaseRule {
    
    private static final int MIN_CURRENT_ABS = 0;
    private static final int MAX_CURRENT_ABS = 20000; // 单位 0.1A，20000表示2000A

    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer current = data.getTotalCurrent(); // 注意：已经计算过偏移
        if (current == null) {
            return noIssue();
        }
        
        int absoluteCurrent = Math.abs(current);
        if (absoluteCurrent < MIN_CURRENT_ABS || absoluteCurrent > MAX_CURRENT_ABS) {
            return singleIssue(data, 
                    String.format("总电流: %d (0.1A)", current));
        }
        
        return noIssue();
    }
} 
