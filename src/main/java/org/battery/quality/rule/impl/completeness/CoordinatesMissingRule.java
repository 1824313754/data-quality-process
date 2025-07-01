package org.battery.quality.rule.impl.completeness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 车辆位置坐标缺失检查规则
 */
@QualityRule(
    type = "COORDINATES_MISSING",
    code = 4003,
    description = "车辆位置坐标缺失",
    category = RuleType.COMPLETENESS,
    priority = 8
)
public class CoordinatesMissingRule extends AbstractRule {
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Long longitude = data.getLongitude();
        Long latitude = data.getLatitude();
        
        // 检查经纬度是否缺失
        if (longitude == null || latitude == null) {
            StringBuilder message = new StringBuilder();
            if (longitude == null) {
                message.append("经度缺失 ");
            }
            if (latitude == null) {
                message.append("纬度缺失");
            }
            return singleIssue(data, message.toString());
        }
        
        // 检查是否为0值
        if (longitude == 0L && latitude == 0L) {
            return singleIssue(data, "经纬度均为0值");
        }
        System.out.println(111);
        return noIssue();
    }
} 
