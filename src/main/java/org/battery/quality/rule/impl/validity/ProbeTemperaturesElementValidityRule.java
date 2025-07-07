package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 温度探针元素有效性检查规则
 */
@QualityRule(
    type = "PROBE_TEMPERATURES_ELEMENT_VALIDITY",
    code = 1008,
    description = "温度探针元素无效",
    category = RuleType.VALIDITY,
    priority = 6
)
public class ProbeTemperaturesElementValidityRule extends AbstractRule {
    
    private static final int MIN_TEMPERATURE = -40;
    private static final int MAX_TEMPERATURE = 250; // 单位 ℃ - 40
    private static final int MAX_INVALID_COUNT = 3; // 最多允许3个无效值

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        List<Integer> temperatures = data.getProbeTemperatures();
        if (temperatures == null || temperatures.isEmpty()) {
            return noIssue();
        }
        
        // 检查每个温度值是否在有效范围内
        List<Integer> invalidIndices = new ArrayList<>();
        for (int i = 0; i < temperatures.size(); i++) {
            Integer temp = temperatures.get(i);
            if (temp == null || temp < MIN_TEMPERATURE || temp > MAX_TEMPERATURE) {
                invalidIndices.add(i);
            }
        }
        
        // 如果无效值超过阈值，则报告问题
        if (!invalidIndices.isEmpty() && invalidIndices.size() > MAX_INVALID_COUNT) {
            String invalidValues = invalidIndices.stream()
                    .map(i -> String.format("index=%d,value=%s", i, temperatures.get(i)))
                    .collect(Collectors.joining("; "));
            
            return singleIssue(data, String.format("无效温度元素数量: %d, 具体: %s", 
                    invalidIndices.size(), invalidValues));
        }
        
        return noIssue();
    }
} 
