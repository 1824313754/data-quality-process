package org.battery.quality.rule.impl.timeliness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 数据延迟检查规则
 */
@QualityRule(
    type = "DATA_DELAY",
    code = 2002,
    description = "数据处理延迟",
    category = RuleType.TIMELINESS,
    priority = 2
)
public class DataDelayRule extends AbstractRule {
    
    private static final long MAX_DELAY_MS = 5 * 60 * 1000; // 最大延迟5分钟

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        String dataTime = data.getTime(); // 数据采集时间
        
        if (dataTime == null) {
            return noIssue();
        }
        
        try {
            // 解析时间字符串
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date timeDate = format.parse(dataTime);
            Date now = new Date();
            
            // 计算延迟时间
            long delay = now.getTime() - timeDate.getTime();
            
            // 如果延迟超过阈值，则报告问题
            if (delay > MAX_DELAY_MS) {
                return singleIssue(data, 
                        String.format("数据延迟: %d毫秒 (%.2f分钟)", 
                                delay, delay / 60000.0));
            }
        } catch (Exception e) {
            // 如果解析出错，记录一个解析错误的问题
            return singleIssue(data, "时间格式解析错误: " + e.getMessage());
        }
        
        return noIssue();
    }
} 
