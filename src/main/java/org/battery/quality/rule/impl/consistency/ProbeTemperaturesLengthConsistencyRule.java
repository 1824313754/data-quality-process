package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractStateRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 温度探针数组长度一致性检查规则
 */
@QualityRule(
    type = "PROBE_TEMPERATURES_LENGTH_CONSISTENCY",
    code = 3003,
    description = "温度探针数组长度不一致",
    category = RuleType.CONSISTENCY,
    priority = 6
)
public class ProbeTemperaturesLengthConsistencyRule extends AbstractStateRule {

    @Override
    protected List<Issue> doCheckState(Gb32960Data currentData, Gb32960Data previousData) {
        // 如果没有前一条数据，则跳过检查
        if (previousData == null) {
            return noIssue();
        }
        
        List<Integer> currentTemps = currentData.getProbeTemperatures();
        List<Integer> previousTemps = previousData.getProbeTemperatures();
        
        // 如果两者都为空或都不为空但长度相同，则正常
        if ((currentTemps == null && previousTemps == null) ||
            (currentTemps != null && previousTemps != null && 
             currentTemps.size() == previousTemps.size())) {
            return noIssue();
        }
        
        // 计算温度数组长度差异
        int currentLength = currentTemps != null ? currentTemps.size() : 0;
        int previousLength = previousTemps != null ? previousTemps.size() : 0;
        
        return singleIssue(currentData, 
                String.format("当前长度: %d, 前一条长度: %d", currentLength, previousLength));
    }
} 
