package org.battery.quality.rule.impl.completeness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 单体电压数组缺失率检查规则
 */
@QualityRule(
    type = "CELL_VOLTAGES_MISSING",
    code = 4002,
    description = "单体电压数组缺失率检查",
    category = RuleType.COMPLETENESS,
    priority = 10
)
public class CellVoltagesMissingRule extends AbstractRule {
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        List<Integer> cellVoltages = data.getCellVoltages();
        
        if (cellVoltages == null || cellVoltages.isEmpty()) {
            return singleIssue(data, "cellVoltages为空");
        }
        
        return noIssue();
    }
} 
