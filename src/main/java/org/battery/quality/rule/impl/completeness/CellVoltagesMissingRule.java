package org.battery.quality.rule.impl.completeness;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 电压数组缺失率检查规则
 */
@RuleDefinition(
    type = "CELL_VOLTAGES_MISSING",
    code = 4003,
    description = "电压数组缺失率检查",
    category = RuleCategory.COMPLETENESS,
    priority = 10
)
public class CellVoltagesMissingRule extends AbstractRule {
    
    @Override
    public List<QualityIssue> check(BatteryData data) {
        List<Integer> cellVoltages = data.getCellVoltages();
        if (cellVoltages == null || cellVoltages.isEmpty()) {
            return singleIssue(data, "cellVoltages为空");
        }
        
        return noIssue();
    }
} 
