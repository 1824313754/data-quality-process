package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractStateRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 单体电压数组长度一致性检查规则
 * 检查同一VIN、batteryNumber下cellVoltages数组长度是否一致
 */
@RuleDefinition(
        type = "CELL_VOLTAGES_LENGTH_CONSISTENCY",
    code = 3004,
    description = "单体电压数组长度不一致",
    category = RuleCategory.CONSISTENCY,
    priority = 6
)
public class CellVoltagesLengthConsistencyRule extends AbstractStateRule {

    @Override
    public List<QualityIssue> checkState(BatteryData currentData, BatteryData previousData) {
        // 如果没有前一条数据，则跳过检查
        if (previousData == null) {
            return noIssue();
        }
        
        List<Integer> currentVoltages = currentData.getCellVoltages();
        List<Integer> previousVoltages = previousData.getCellVoltages();
        
        // 如果两者都为空或都不为空但长度相同，则正常
        if ((currentVoltages == null && previousVoltages == null) ||
            (currentVoltages != null && previousVoltages != null && 
             currentVoltages.size() == previousVoltages.size())) {
            return noIssue();
        }
        
        // 计算电压数组长度差异
        int currentLength = currentVoltages != null ? currentVoltages.size() : 0;
        int previousLength = previousVoltages != null ? previousVoltages.size() : 0;
        
        return singleIssue(currentData, 
                String.format("当前长度: %d, 前一条长度: %d", currentLength, previousLength));
    }
} 
