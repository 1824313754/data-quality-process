package org.battery.quality.rule.impl.timeliness;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractStateRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.text.SimpleDateFormat;
import java.util.Date;
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
public class TimestampMonotonicityRule extends AbstractStateRule {

    @Override
    protected List<Issue> doCheckState(Gb32960Data currentData, Gb32960Data previousData) {
        if (previousData == null || currentData.getCtime() == null || previousData.getCtime() == null) {
            return noIssue();
        }
        
        try {
            // 解析时间字符串
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date currentDate = format.parse(currentData.getCtime());
            Date previousDate = format.parse(previousData.getCtime());
            
            long currentTime = currentDate.getTime();
            long previousTime = previousDate.getTime();
            
            // 检查时间戳是否单调递增
            if (currentTime <= previousTime) {
                return singleIssue(currentData, 
                        String.format("当前时间戳(%s)小于等于前一条记录的时间戳(%s)", 
                                currentData.getCtime(), previousData.getCtime()));
            }
        } catch (Exception e) {
            // 如果解析出错，记录一个解析错误的问题
            return singleIssue(currentData, "时间格式解析错误: " + e.getMessage());
        }
        
        return noIssue();
    }
} 
