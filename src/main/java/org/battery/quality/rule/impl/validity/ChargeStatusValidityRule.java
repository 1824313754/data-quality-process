package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 充电状态有效性检查规则
 */
@QualityRule(
    type = "CHARGE_STATUS_VALIDITY",
    code = 1001,
    description = "充电状态无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class ChargeStatusValidityRule extends AbstractRule {
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer chargeStatus = data.getChargeStatus();
        
        if (chargeStatus == null) {
            return noIssue();
        }
        
        // 充电状态：1-停车充电，2-行驶充电，3-未充电状态，4-充电完成
        if (chargeStatus < 1 || chargeStatus > 4) {
            return singleIssue(data, String.format("充电状态值: %d", chargeStatus));
        }
        
        return noIssue();
    }
} 
