package org.battery.quality.rule.impl.completeness;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * 经纬度缺失检查规则
 */
@RuleDefinition(
    type = "COORDINATES_MISSING",
    code = 4002,
    description = "经纬度缺失",
    category = RuleCategory.COMPLETENESS,
    priority = 8
)
public class CoordinatesMissingRule extends AbstractRule {
    
    @Override
    public List<QualityIssue> check(BatteryData data) {
        Long longitude = data.getLongitude();
        Long latitude = data.getLatitude();
        
        // 经纬度为null，或最大值、最大值-1表示缺失
        boolean isLongitudeMissing = longitude == null || longitude == 0L || 
                                     longitude == Long.MAX_VALUE || longitude == Long.MAX_VALUE - 1;
        boolean isLatitudeMissing = latitude == null || latitude == 0L || 
                                    latitude == Long.MAX_VALUE || latitude == Long.MAX_VALUE - 1;

        if (isLongitudeMissing && isLatitudeMissing) {
            return singleIssue(data, "经度和纬度均缺失");
        } else if (isLongitudeMissing) {
            return singleIssue(data, "经度缺失");
        } else if (isLatitudeMissing) {
            return singleIssue(data, "纬度缺失");
        }
        return noIssue();
    }
} 
