package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 直流状态有效性检查规则
 */
@QualityRule(
    type = "DC_STATUS_VALIDITY",
    code = 1002,
    description = "直流状态无效",
    category = RuleType.VALIDITY,
    priority = 4
)
public class DcStatusValidityRule extends AbstractRule {
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer dcStatus = data.getDcStatus();
        
        if (dcStatus == null) {
            return noIssue();
        }
        
        // 直流状态：0-断开，1-连接
        if (dcStatus != 0 && dcStatus != 1) {
            return singleIssue(data, String.format("直流状态值: %d", dcStatus));
        }
        
        return noIssue();
    }
} 
