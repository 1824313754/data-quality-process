package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 最低温度有效性检查规则
 */
@QualityRule(
    type = "MIN_TEMPERATURE_VALIDITY",
    code = 1006,
    description = "最低温度无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class MinTemperatureValidityRule extends AbstractRule {
    
    private static final int MIN_TEMPERATURE = -40;
    private static final int MAX_TEMPERATURE = 250; // 单位 ℃ - 40

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer temperature = data.getMinTemperature();
        if (temperature == null) {
            return noIssue();
        }
        
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            // 原始数据已经减去40了，这里显示实际温度
            return singleIssue(data, 
                    String.format("最低温度: %d", temperature));
        }
        
        return noIssue();
    }
} 
