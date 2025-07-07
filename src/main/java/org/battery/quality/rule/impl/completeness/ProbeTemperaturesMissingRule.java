package org.battery.quality.rule.impl.completeness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 温度数组缺失率检查规则
 */
@QualityRule(
    type = "PROBE_TEMPERATURES_MISSING",
    code = 4001,
    description = "温度数组缺失率检查",
    category = RuleType.COMPLETENESS,
    priority = 10
)
public class ProbeTemperaturesMissingRule extends AbstractRule {
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        List<Integer> probeTemperatures = data.getProbeTemperatures();
        
        if (probeTemperatures == null || probeTemperatures.isEmpty()) {
            return singleIssue(data, "probeTemperatures为空");
        }
        
        return noIssue();
    }
} 
