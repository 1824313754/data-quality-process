package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 总电流有效性检查规则
 */
@RuleDefinition(
    type = "TOTAL_CURRENT_VALIDITY",
    code = 1004,
    description = "总电流无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class TotalCurrentValidityRule extends AbstractRule {
    
    private static final int MIN_CURRENT_ABS = 0;
    private static final int MAX_CURRENT_ABS = 20000; // 单位 0.1A，20000表示2000A

    @Override
    public List<QualityIssue> check(BatteryData data) {
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
