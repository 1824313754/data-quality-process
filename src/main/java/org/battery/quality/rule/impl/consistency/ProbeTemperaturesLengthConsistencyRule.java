package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseStateRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 温感数组长度一致性检查规则
 */
@QualityRule(
    type = "PROBE_TEMPERATURES_LENGTH_CONSISTENCY",
    code = 3003,
    description = "温感数组长度不一致",
    category = RuleType.CONSISTENCY,
    priority = 5
)
public class ProbeTemperaturesLengthConsistencyRule extends BaseStateRule {

    @Override
    public List<Issue> checkState(Gb32960Data current, Gb32960Data previous) {
        // 没有前一条数据，无法进行比较
        if (previous == null) {
            return noIssue();
        }
        
        List<Integer> currentTemperatures = current.getProbeTemperatures();
        List<Integer> previousTemperatures = previous.getProbeTemperatures();
        
        // 如果任一为null，则不进行比较
        if (currentTemperatures == null || previousTemperatures == null) {
            return noIssue();
        }
        
        // 检查长度是否一致
        if (currentTemperatures.size() != previousTemperatures.size()) {
            return singleIssue(current, 
                    String.format("当前长度: %d, 前一条长度: %d", 
                            currentTemperatures.size(), previousTemperatures.size()));
        }
        return noIssue();
    }
} 
