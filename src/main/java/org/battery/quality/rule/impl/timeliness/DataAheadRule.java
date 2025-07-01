package org.battery.quality.rule.impl.timeliness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        String ctimeStr = data.getCtime();
        String timeStr = data.getTime();
        
        if (ctimeStr == null || timeStr == null) {
            return noIssue();
        }
        
        try {
            // 解析时间字符串
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date timeDate = format.parse(timeStr);
            Date ctimeDate = format.parse(ctimeStr);
            
            long time = timeDate.getTime();
            long ctime = ctimeDate.getTime();
            
            long diff = time - ctime;
            
            // 如果时间差小于-1分钟（数据时间超前系统时间1分钟以上）
            if (diff < MIN_AHEAD_MS) {
                return singleIssue(data, 
                        String.format("时间超前: %d毫秒, %.2f分钟", 
                                Math.abs(diff), Math.abs(diff) / 60000.0));
            }
        } catch (Exception e) {
            // 如果解析出错，记录一个解析错误的问题
            return singleIssue(data, "时间格式解析错误: " + e.getMessage());
        }
        
        return noIssue();
    }
} 
