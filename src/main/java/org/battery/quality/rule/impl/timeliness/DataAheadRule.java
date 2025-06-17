package org.battery.quality.rule.impl.timeliness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.util.List;

/**
 * 数据时间超前检查规则
 */
@QualityRule(
    type = "DATA_AHEAD",
    code = 2003,
    description = "数据时间超前",
    category = RuleType.TIMELINESS,
    priority = 13
)
public class DataAheadRule extends BaseRule {
    
    private static final long MIN_AHEAD_MS = -60 * 1000; // -1分钟，单位毫秒

    @Override
    public List<Issue> check(Gb32960Data data) {
        Long ctime = data.getCtime();
        if (ctime == null) {
            return noIssue();
        }
        
        long systemTime = System.currentTimeMillis();
        long diff = systemTime - ctime;
        
        // 如果时间差小于-1分钟（数据时间超前系统时间1分钟以上）
        if (diff < MIN_AHEAD_MS) {
            return singleIssue(data, 
                    String.format("时间超前: %d毫秒, %.2f分钟", 
                            Math.abs(diff), Math.abs(diff) / 60000.0));
        }
        
        return noIssue();
    }
} 
