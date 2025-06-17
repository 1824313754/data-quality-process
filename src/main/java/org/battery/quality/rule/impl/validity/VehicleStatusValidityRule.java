package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 车辆状态有效性检查规则
 */
@QualityRule(
    type = "VEHICLE_STATUS_VALIDITY",
    code = 1001,
    description = "车辆状态无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class VehicleStatusValidityRule extends BaseRule {

    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer status = data.getVehicleStatus();
        if (status == null) {
            return noIssue();
        }
        
        // 车辆状态: 1-启动，2-熄火，3-其他
        if (status < 1 || status > 3) {
            return singleIssue(data, 
                    String.format("车辆状态: %d", status));
        }
        
        return noIssue();
    }
} 
