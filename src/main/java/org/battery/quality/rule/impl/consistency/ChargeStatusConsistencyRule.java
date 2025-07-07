package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 充电状态一致性检查规则
 */
@QualityRule(
    type = "CHARGE_STATUS_CONSISTENCY",
    code = 3002,
    description = "充电状态与车辆状态不一致",
    category = RuleType.CONSISTENCY,
    priority = 3
)
public class ChargeStatusConsistencyRule extends AbstractRule {
    
    // 车辆状态：0-停车充电，1-行驶充电，2-停车不充电，3-行驶不充电
    private static final int VEHICLE_STATUS_PARKING_CHARGING = 0;
    private static final int VEHICLE_STATUS_DRIVING_CHARGING = 1;
    private static final int VEHICLE_STATUS_PARKING_NOT_CHARGING = 2;
    private static final int VEHICLE_STATUS_DRIVING_NOT_CHARGING = 3;
    
    // 充电状态：1-停车充电，2-行驶充电，3-未充电状态，4-充电完成
    private static final int CHARGE_STATUS_PARKING_CHARGING = 1;
    private static final int CHARGE_STATUS_DRIVING_CHARGING = 2;
    private static final int CHARGE_STATUS_NOT_CHARGING = 3;
    private static final int CHARGE_STATUS_COMPLETED = 4;
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer vehicleStatus = data.getVehicleStatus();
        Integer chargeStatus = data.getChargeStatus();
        
        if (vehicleStatus == null || chargeStatus == null) {
            return noIssue();
        }
        
        boolean isConsistent = checkConsistency(vehicleStatus, chargeStatus);
        
        if (!isConsistent) {
            return singleIssue(data, 
                    String.format("车辆状态(%d)与充电状态(%d)不一致", 
                            vehicleStatus, chargeStatus));
        }
        
        return noIssue();
    }
    
    private boolean checkConsistency(int vehicleStatus, int chargeStatus) {
        switch (vehicleStatus) {
            case VEHICLE_STATUS_PARKING_CHARGING:
                return chargeStatus == CHARGE_STATUS_PARKING_CHARGING || 
                       chargeStatus == CHARGE_STATUS_COMPLETED;
                
            case VEHICLE_STATUS_DRIVING_CHARGING:
                return chargeStatus == CHARGE_STATUS_DRIVING_CHARGING;
                
            case VEHICLE_STATUS_PARKING_NOT_CHARGING:
            case VEHICLE_STATUS_DRIVING_NOT_CHARGING:
                return chargeStatus == CHARGE_STATUS_NOT_CHARGING;
                
            default:
                return false;
        }
    }
} 
