package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 单体电压数组元素值有效性检查规则
 * 检查cellVoltages数组中每个元素是否在有效范围内
 */
@RuleDefinition(
    type = "CELL_VOLTAGES_ELEMENT_VALIDITY",
    code = 1016,
    description = "单体电压数组存在无效值",
    category = RuleCategory.VALIDITY,
    priority = 3
)
public class CellVoltagesElementValidityRule extends AbstractRule {
    
    private static final int MIN_VOLTAGE = 0;
    private static final int MAX_VOLTAGE = 60000; // 根据指标表：元素 ∉ [0,60000]

    @Override
    public List<QualityIssue> check(BatteryData data) {
        List<Integer> voltages = data.getCellVoltages();
        if (voltages == null || voltages.isEmpty()) {
            return noIssue();
        }
        
        // 查找无效的电压值及其索引
        List<String> invalidEntries = IntStream.range(0, voltages.size())
                .filter(i -> {
                    Integer voltage = voltages.get(i);
                    return voltage == null || 
                           voltage < MIN_VOLTAGE || 
                           voltage > MAX_VOLTAGE;
                })
                .mapToObj(i -> String.format("[%d]=%s", i, voltages.get(i)))
                .collect(Collectors.toList());
                
        if (!invalidEntries.isEmpty()) {
            // 最多展示前10个异常值
            String invalidValues = String.join(", ", 
                    invalidEntries.subList(0, Math.min(invalidEntries.size(), 10)));
            
            if (invalidEntries.size() > 10) {
                invalidValues += String.format(" ... (共%d个无效值)", invalidEntries.size());
            }
            
            return singleIssue(data, invalidValues);
        }
        
        return noIssue();
    }
}
