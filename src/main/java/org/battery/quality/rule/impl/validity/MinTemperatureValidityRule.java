package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 最小温度有效性检查规则
 */
@QualityRule(
    type = "MIN_TEMPERATURE_VALIDITY",
    code = 1013,
    description = "最小温度无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class MinTemperatureValidityRule extends BaseRule {
    
    private static final int MIN_TEMPERATURE = 0;
    private static final int MAX_TEMPERATURE = 250; // 单位 ℃ - 40

    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer temperature = data.getMinTemperature();
        if (temperature == null) {
            return noIssue();
        }
        
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            // 原始数据已经减去40了，这里显示实际温度
            return singleIssue(data, 
                    String.format("最小温度: %d", temperature));
        }
        
        return noIssue();
    }
} 
