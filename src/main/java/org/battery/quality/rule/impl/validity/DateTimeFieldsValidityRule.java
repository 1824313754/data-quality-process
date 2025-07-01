package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.template.AbstractRule;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.annotation.QualityRule;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 日期时间字段有效性检查规则
 */
@QualityRule(
    type = "DATE_TIME_FIELDS_VALIDITY",
    code = 1014,
    description = "日期时间字段无效",
    category = RuleType.VALIDITY,
    priority = 10
)
public class DateTimeFieldsValidityRule extends AbstractRule {

    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        Integer year = data.getYear();
        Integer month = data.getMonth();
        Integer day = data.getDay();
        Integer hours = data.getHours();
        Integer minutes = data.getMinutes();
        Integer seconds = data.getSeconds();
        
        List<String> invalidFields = new ArrayList<>();
        
        // 检查各个时间字段是否在有效范围内
        if (year == null || year < 2000 || year > 2100) {
            invalidFields.add(String.format("年份(%s)", year));
        }
        
        if (month == null || month < 1 || month > 12) {
            invalidFields.add(String.format("月份(%s)", month));
        }
        
        if (day == null || day < 1 || day > 31) {
            invalidFields.add(String.format("日期(%s)", day));
        }
        
        if (hours == null || hours < 0 || hours > 23) {
            invalidFields.add(String.format("小时(%s)", hours));
        }
        
        if (minutes == null || minutes < 0 || minutes > 59) {
            invalidFields.add(String.format("分钟(%s)", minutes));
        }
        
        if (seconds == null || seconds < 0 || seconds > 59) {
            invalidFields.add(String.format("秒(%s)", seconds));
        }
        
        // 如果有字段无效，则返回问题
        if (!invalidFields.isEmpty()) {
            return singleIssue(data, String.join(", ", invalidFields));
        }
        
        // 检查日期是否合法（如2月30日）
        try {
            LocalDateTime.of(year, month, day, hours, minutes, seconds);
        } catch (DateTimeException e) {
            return singleIssue(data, 
                    String.format("日期时间组合无效: %04d-%02d-%02d %02d:%02d:%02d (%s)", 
                            year, month, day, hours, minutes, seconds, e.getMessage()));
        }
        
        return noIssue();
    }
} 