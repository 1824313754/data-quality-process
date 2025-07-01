package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 车辆状态有效性检查规则
 */
@QualityRule(
    type = "VEHICLE_STATUS_VALIDITY",
    code = 1013,
    description = "车辆状态无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class VehicleStatusValidityRule extends AbstractRule {
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer status = data.getVehicleStatus();
        
        if (status == null) {
            return noIssue();
        }
        
        // 车辆状态：0-停车充电，1-行驶充电，2-停车不充电，3-行驶不充电，其他为无效值
        if (status < 0 || status > 3) {
            return singleIssue(data, String.format("车辆状态值: %d", status));
        }
        
        return noIssue();
    }
} 
