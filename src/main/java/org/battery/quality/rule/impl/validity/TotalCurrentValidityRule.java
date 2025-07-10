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
    
    private static final int MIN_CURRENT = -1000; // -1000A
    private static final int MAX_CURRENT = 1000;  // 1000A

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer current = data.getTotalCurrent(); // 注意：已经计算过偏移
        if (current == null) {
            return noIssue();
        }

        // 总电流取值范围: [-1000A, 1000A] 根据指标表修正
        if (current < MIN_CURRENT || current > MAX_CURRENT) {
            return singleIssue(data,
                    String.format("总电流: %dA", current));
        }

        return noIssue();
    }
} 
