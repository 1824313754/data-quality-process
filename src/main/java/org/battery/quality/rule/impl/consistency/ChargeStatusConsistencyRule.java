package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 充电状态一致性检查规则
 */
@RuleDefinition(
    type = "CHARGE_STATUS_CONSISTENCY",
    code = 3002,
    description = "充电状态与电流不一致",
    category = RuleCategory.CONSISTENCY,
    priority = 8
)
public class ChargeStatusConsistencyRule extends AbstractRule {

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer current = data.getTotalCurrent(); // 已经计算过偏移，<0表示放电，>0表示充电
        Integer chargeStatus = data.getChargeStatus();
        
        if (current == null || chargeStatus == null) {
            return noIssue();
        }
        
        // 充电状态：1-停车充电，2-行驶充电，3-未充电状态，4-充电完成
        if (current > 0 && !(chargeStatus == 1 || chargeStatus == 2)) {
            return singleIssue(data, 
                    String.format("电流为%d（充电），但充电状态为%d", current, chargeStatus));
        }
        
        if (current < 0 && !(chargeStatus == 3 || chargeStatus == 4)) {
            return singleIssue(data, 
                    String.format("电流为%d（放电），但充电状态为%d", current, chargeStatus));
        }
        
        return noIssue();
    }
} 
