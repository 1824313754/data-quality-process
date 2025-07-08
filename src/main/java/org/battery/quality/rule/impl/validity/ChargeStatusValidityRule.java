package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 充电状态有效性检查规则
 */
@RuleDefinition(
    type = "CHARGE_STATUS_VALIDITY",
    code = 1005,
    description = "充电状态无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class ChargeStatusValidityRule extends AbstractRule {

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer status = data.getChargeStatus();
        if (status == null) {
            return noIssue();
        }
        
        // 充电状态：1-停车充电，2-行驶充电，3-未充电状态，4-充电完成
        if (status < 1 || status > 4) {
            return singleIssue(data, 
                    String.format("充电状态: %d", status));
        }
        
        return noIssue();
    }
} 
