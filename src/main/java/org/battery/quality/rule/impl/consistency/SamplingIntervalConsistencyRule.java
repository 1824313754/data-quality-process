package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractStateRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 采样间隔一致性检查规则
 */
@QualityRule(
    type = "SAMPLING_INTERVAL_CONSISTENCY",
    code = 3001,
    description = "采样间隔不一致",
    category = RuleType.CONSISTENCY,
    priority = 4
)
public class SamplingIntervalConsistencyRule extends AbstractStateRule {
    
    private static final long MIN_INTERVAL_MS = 1000; // 最小间隔1秒
    private static final long MAX_INTERVAL_MS = 60 * 60 * 1000; // 最大间隔1小时
    
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
            
            // 计算时间间隔（毫秒）
            long interval = currentTime - previousTime;
            
            // 检查时间间隔是否在合理范围内
            if (interval < MIN_INTERVAL_MS || interval > MAX_INTERVAL_MS) {
                return singleIssue(currentData, 
                        String.format("采样间隔异常: %d毫秒", interval));
            }
        } catch (Exception e) {
            // 如果解析出错，记录一个解析错误的问题
            return singleIssue(currentData, "时间格式解析错误: " + e.getMessage());
        }
        
        return noIssue();
    }
} 
