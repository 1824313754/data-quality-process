package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * DC状态有效性检查规则
 */
@QualityRule(
    type = "DC_STATUS_VALIDITY",
    code = 1008,
    description = "DC状态无效",
    category = RuleType.VALIDITY,
    priority = 5
)
public class DcStatusValidityRule extends BaseRule {

    @Override
    public List<Issue> check(Gb32960Data data) {
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
