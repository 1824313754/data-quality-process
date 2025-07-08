package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 车辆状态有效性检查规则
 */
@RuleDefinition(
    type = "VEHICLE_STATUS_VALIDITY",
    code = 1001,
    description = "车辆状态无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class VehicleStatusValidityRule extends AbstractRule {

    @Override
    public List<QualityIssue> check(BatteryData data) {
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
