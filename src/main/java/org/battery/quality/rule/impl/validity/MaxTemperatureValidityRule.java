package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 最大温度有效性检查规则
 */
@RuleDefinition(
    type = "MAX_TEMPERATURE_VALIDITY",
    code = 1012,
    description = "最大温度无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class MaxTemperatureValidityRule extends AbstractRule {
    
    private static final int MIN_TEMPERATURE = 0;
    private static final int MAX_TEMPERATURE = 210; // 单位 ℃，根据指标表修正

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer temperature = data.getMaxTemperature();
        if (temperature == null) {
            return noIssue();
        }

        // 最大温度取值范围: [0, 210] 根据指标表
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            return singleIssue(data,
                    String.format("最大温度: %d℃", temperature));
        }

        return noIssue();
    }
} 
