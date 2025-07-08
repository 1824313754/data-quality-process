package org.battery.quality.rule.impl.completeness;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 温度数组缺失率检查规则
 */
@RuleDefinition(
    type = "PROBE_TEMPERATURES_MISSING",
    code = 4001,
    description = "温度数组缺失率检查",
    category = RuleCategory.COMPLETENESS,
    priority = 10
)
public class ProbeTemperaturesMissingRule extends AbstractRule {
    
    @Override
    public List<QualityIssue> check(BatteryData data) {
        List<Integer> probeTemperatures = data.getProbeTemperatures();
        
        if (probeTemperatures == null || probeTemperatures.isEmpty()) {
            return singleIssue(data, "probeTemperatures为空");
        }
        
        return noIssue();
    }
} 
