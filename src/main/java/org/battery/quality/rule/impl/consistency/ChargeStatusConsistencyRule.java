package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 充电状态一致性检查规则
 */
@QualityRule(
    type = "CHARGE_STATUS_CONSISTENCY",
    code = 3002,
    description = "充电状态与电流不一致",
    category = RuleType.CONSISTENCY,
    priority = 8
)
public class ChargeStatusConsistencyRule extends BaseRule {

    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer current = data.getCurrent(); // 已经计算过偏移，<0表示放电，>0表示充电
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
//        System.out.println(111);
        return noIssue();
    }
} 
