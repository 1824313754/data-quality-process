package org.battery.quality.rule.impl.timeliness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseStateRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 时间戳单调性检查规则
 */
@QualityRule(
    type = "TIMESTAMP_MONOTONICITY",
    code = 2001,
    description = "时间戳非单调递增",
    category = RuleType.TIMELINESS,
    priority = 7
)
public class TimestampMonotonicityRule extends BaseStateRule {

    @Override
    public List<Issue> checkState(Gb32960Data currentData, Gb32960Data previousData) {
        if (previousData == null || currentData.getCtime() == null || previousData.getCtime() == null) {
            return noIssue();
        }
        
        // 检查时间戳是否单调递增
        if (currentData.getCtime() <= previousData.getCtime()) {
            return singleIssue(currentData, 
                    String.format("当前时间戳(%d)小于等于前一条记录的时间戳(%d)", 
                            currentData.getCtime(), previousData.getCtime()));
        }
        
        return noIssue();
    }
} 
