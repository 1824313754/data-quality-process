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
 * 数据延迟度检查规则
 */
@QualityRule(
    type = "DATA_DELAY",
    code = 2002,
    description = "数据延迟度过高",
    category = RuleType.TIMELINESS,
    priority = 12
)
public class DataDelayRule extends BaseRule {
    
    private static final long MAX_DELAY_MS = 10 * 60 * 1000; // 10分钟，单位毫秒

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
            
            long delay = time - ctime;
            
            if (delay > MAX_DELAY_MS) {
                return singleIssue(data, 
                        String.format("延迟: %d毫秒, %.2f分钟", 
                                delay, delay / 60000.0));
            }
        } catch (Exception e) {
            // 如果解析出错，记录一个解析错误的问题
            return singleIssue(data, "时间格式解析错误: " + e.getMessage());
        }
        
        return noIssue();
    }
} 
