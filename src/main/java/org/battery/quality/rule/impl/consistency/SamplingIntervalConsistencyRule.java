package org.battery.quality.rule.impl.consistency;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseStateRule;
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
    description = "相邻记录采样间隔异常",
    category = RuleType.CONSISTENCY,
    priority = 7
)
public class SamplingIntervalConsistencyRule extends BaseStateRule {
    
    private static final long NORMAL_INTERVAL = 10 * 1000; // 10秒，单位毫秒
    private static final long MAX_DEVIATION = 10 * 1000; // 允许偏差10秒

    @Override
    public List<Issue> checkState(Gb32960Data current, Gb32960Data previous) {
        // 没有前一条数据，无法进行比较
        if (previous == null) {
            return noIssue();
        }
        
        // 确保是同一辆车
        if (!isSameVehicle(current, previous)) {
            return noIssue();
        }
        
        // 检查采样间隔
        String currentTimeStr = current.getCtime();
        String previousTimeStr = previous.getCtime();
        
        if (currentTimeStr != null && previousTimeStr != null) {
            try {
                // 解析时间字符串
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date currentDate = format.parse(currentTimeStr);
                Date previousDate = format.parse(previousTimeStr);
                
                long currentTime = currentDate.getTime();
                long previousTime = previousDate.getTime();
                
                long interval = currentTime - previousTime;
                long deviation = Math.abs(interval - NORMAL_INTERVAL);
                
                if (deviation > MAX_DEVIATION) {
                    return singleIssue(current, 
                            String.format("采样间隔: %d毫秒, 偏差: %d毫秒", 
                                    interval, deviation));
                }
            } catch (Exception e) {
                // 如果解析出错，记录一个解析错误的问题
                return singleIssue(current, "时间格式解析错误: " + e.getMessage());
            }
        }
        
        return noIssue();
    }
    
    /**
     * 判断是否为同一辆车
     */
    private boolean isSameVehicle(Gb32960Data current, Gb32960Data previous) {
        return current.getVin() != null && current.getVin().equals(previous.getVin());
    }
} 
