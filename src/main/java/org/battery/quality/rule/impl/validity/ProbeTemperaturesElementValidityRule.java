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
 * 温度数组元素值有效性检查规则
 */
@RuleDefinition(
    type = "PROBE_TEMPERATURES_ELEMENT_VALIDITY",
    code = 1015,
    description = "温度数组存在无效值",
    category = RuleCategory.VALIDITY,
    priority = 3
)
public class ProbeTemperaturesElementValidityRule extends AbstractRule {
    
    private static final int MIN_TEMPERATURE = 0;
    private static final int MAX_TEMPERATURE = 250; // 单位 ℃ - 40

    @Override
    public List<QualityIssue> check(BatteryData data) {
        List<Integer> temperatures = data.getProbeTemperatures();
        if (temperatures == null || temperatures.isEmpty()) {
            return noIssue();
        }
        
        // 查找无效的温度值及其索引
        List<String> invalidEntries = IntStream.range(0, temperatures.size())
                .filter(i -> {
                    Integer temperature = temperatures.get(i);
                    return temperature == null || 
                           temperature < MIN_TEMPERATURE || 
                           temperature > MAX_TEMPERATURE;
                })
                .mapToObj(i -> String.format("[%d]=%s", i, temperatures.get(i)))
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
