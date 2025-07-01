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
 * 数据超前检查规则
 */
@QualityRule(
    type = "DATA_AHEAD",
    code = 2001,
    description = "数据时间超前于当前时间",
    category = RuleType.TIMELINESS,
    priority = 3
)
public class DataAheadRule extends AbstractRule {
    
    private static final long MAX_AHEAD_MS = 10 * 60 * 1000; // 最大超前10分钟

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        if (data.getCtime() == null) {
            return noIssue();
        }
        
        try {
            // 解析时间字符串
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dataTime = format.parse(data.getCtime());
            Date now = new Date();
            
            // 检查数据时间是否超前于当前时间
            if (dataTime.getTime() > now.getTime()) {
                long diff = dataTime.getTime() - now.getTime();
                
                // 如果超前时间超过阈值，则报告问题
                if (diff > MAX_AHEAD_MS) {
                    return singleIssue(data, 
                            String.format("数据时间(%s)超前于当前时间(%s) %d毫秒", 
                                    data.getCtime(), format.format(now), diff));
                }
            }
        } catch (Exception e) {
            // 如果解析出错，记录一个解析错误的问题
            return singleIssue(data, "时间格式解析错误: " + e.getMessage());
        }
        
        return noIssue();
    }
} 
