package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.util.List;

/**
 * DC状态有效性检查规则
 */
@RuleDefinition(
    type = "DC_STATUS_VALIDITY",
    code = 1008,
    description = "DC状态无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class DcStatusValidityRule extends AbstractRule {

    @Override
    public List<QualityIssue> check(BatteryData data) {
        Integer status = data.getDcStatus();
        if (status == null) {
            return noIssue();
        }
        
        // DC状态: 1-工作，2-断开
        if (status < 1 || status > 2) {
            return singleIssue(data, 
                    String.format("DC状态: %d", status));
        }
        
        return noIssue();
    }
} 
