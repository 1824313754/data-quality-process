package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 最大温度有效性检查规则
 */
@QualityRule(
    type = "MAX_TEMPERATURE_VALIDITY",
    code = 1012,
    description = "最大温度无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class MaxTemperatureValidityRule extends AbstractRule {
    
    private static final int MIN_TEMPERATURE = 0;
    private static final int MAX_TEMPERATURE = 250; // 单位 ℃ - 40

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer temperature = data.getMaxTemperature();
        if (temperature == null) {
            return noIssue();
        }
        
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            // 原始数据已经减去40了，这里显示实际温度
            return singleIssue(data, 
                    String.format("最大温度: %d", temperature));
        }
        
        return noIssue();
    }
} 
