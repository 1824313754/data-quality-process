package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.time.Year;
import java.util.List;

/**
 * 采集时间有效性检查规则
 */
@QualityRule(
    type = "COLLECTION_TIME_VALIDITY",
    code = 1014,
    description = "采集时间无效",
    category = RuleType.VALIDITY,
    priority = 10
)
public class CollectionTimeValidityRule extends BaseRule {

    @Override
    public List<Issue> check(Gb32960Data data) {
        Integer year = data.getYear();
        Integer month = data.getMonth();
        Integer day = data.getDay();
        Integer hours = data.getHours();
        Integer minutes = data.getMinutes();
        Integer seconds = data.getSeconds();
        Long ctime = data.getCtime();
        
        // 检查基本时间字段是否合法
        boolean dateFieldsInvalid = isDateTimeFieldsInvalid(year, month, day, hours, minutes, seconds);
        
        // 检查时间戳是否合法（不为空且为正数）
        boolean timestampInvalid = ctime == null || ctime <= 0;
        
        if (dateFieldsInvalid || timestampInvalid) {
            return singleIssue(data, 
                    String.format("日期时间: %04d-%02d-%02d %02d:%02d:%02d, 时间戳: %s", 
                            year == null ? 0 : year, 
                            month == null ? 0 : month, 
                            day == null ? 0 : day, 
                            hours == null ? 0 : hours, 
                            minutes == null ? 0 : minutes, 
                            seconds == null ? 0 : seconds,
                            ctime));
        }
        
        return noIssue();
    }
    
    /**
     * 检查日期时间字段是否无效
     */
    private boolean isDateTimeFieldsInvalid(Integer year, Integer month, Integer day, 
                                           Integer hours, Integer minutes, Integer seconds) {
        // 任一为空则无效
        if (year == null || month == null || day == null || 
            hours == null || minutes == null || seconds == null) {
            return true;
        }
        
        // 检查范围
        if (year < 2000 || year > Year.now().getValue() + 1) {
            return true;
        }
        
        if (month < 1 || month > 12) {
            return true;
        }
        
        int maxDays = 31;
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            maxDays = 30;
        } else if (month == 2) {
            // 简单判断闰年
            maxDays = ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) ? 29 : 28;
        }
        
        if (day < 1 || day > maxDays) {
            return true;
        }
        
        if (hours < 0 || hours > 23) {
            return true;
        }
        
        if (minutes < 0 || minutes > 59) {
            return true;
        }
        
        if (seconds < 0 || seconds > 59) {
            return true;
        }
        
        return false;
    }
} 
