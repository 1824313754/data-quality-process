package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.model.RuleType;
import org.battery.quality.rule.BaseRule;
import org.battery.quality.rule.annotation.QualityRule;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 时间字段有效性检查规则
 * 检查year、month、day、hours、minutes、seconds等字段是否有效且可以构成一个合法的时间
 */
@QualityRule(
        type = "COLLECTION_TIME_VALIDITY",
        code = 1014,
        description = "采集时间无效",
        category = RuleType.VALIDITY,
        priority = 20
)
public class DateTimeFieldsValidityRule extends BaseRule {

    @Override
    public List<Issue> check(Gb32960Data data) {
        List<Issue> issues = new ArrayList<>();
        // 不再尝试将时间字符串转换为时间戳
        String timeStr = data.getTime();
        
        // 检查时间字段的值是否在有效范围内
        Integer year = data.getYear();
        Integer month = data.getMonth();
        Integer day = data.getDay();
        Integer hours = data.getHours();
        Integer minutes = data.getMinutes();
        Integer seconds = data.getSeconds();
        
        // 检查所有字段的有效性
        if ((!isValidRange(year, 0, 99)) ||
            !isValidRange(month, 1, 12) ||
            !isValidRange(day, 1, 31) ||
            !isValidRange(hours, 0, 23) ||
            !isValidRange(minutes, 0, 59) ||
            !isValidRange(seconds, 0, 59)) {
            
            StringBuilder invalidFields = new StringBuilder();
            
            if (!isValidRange(year, 0, 99)) {
                invalidFields.append("年份值无效: ").append(year).append("; ");
            }
            if (!isValidRange(month, 1, 12)) {
                invalidFields.append("月份值无效: ").append(month).append("; ");
            }
            if (!isValidRange(day, 1, 31)) {
                invalidFields.append("日期值无效: ").append(day).append("; ");
            }
            if (!isValidRange(hours, 0, 23)) {
                invalidFields.append("小时值无效: ").append(hours).append("; ");
            }
            if (!isValidRange(minutes, 0, 59)) {
                invalidFields.append("分钟值无效: ").append(minutes).append("; ");
            }
            if (!isValidRange(seconds, 0, 59)) {
                invalidFields.append("秒钟值无效: ").append(seconds).append("; ");
            }
            
            issues.add(Issue.builder()
                .code(getCode())
                .value("时间字段值无效: " + invalidFields)
                .build());
            
            // 使用当前时间作为ctime
            data.setCtime(getCurrentTimeAsString());
            return issues;
        }

        int fullYear = (year < 100) ? (2000 + year) : year;
        try {
            LocalDateTime dateTime = LocalDateTime.of(fullYear, month, day, hours, minutes, seconds);
            
            // 计算并设置ctime，直接格式化为字符串
            data.setCtime(String.format("%04d-%02d-%02d %02d:%02d:%02d", 
                fullYear, month, day, hours, minutes, seconds));
            
        } catch (Exception e) {
            issues.add(Issue.builder()
                .code(getCode())
                .value("无法构造有效的日期时间: " + e.getMessage())
                .build());
            
            // 使用当前时间作为ctime
            data.setCtime(getCurrentTimeAsString());
            return issues;
        }
        
        return noIssue();
    }
    
    /**
     * 获取当前时间作为格式化的字符串
     */
    private String getCurrentTimeAsString() {
        java.util.Date now = new java.util.Date();
        return String.format("%tF %tT", now, now);
    }

    /**
     * 检查值是否在指定范围内
     */
    private boolean isValidRange(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }
    
    /**
     * 创建质量问题
     */
    protected Issue createIssue(Gb32960Data data, String description) {
        return Issue.builder()
            .code(getCode())
            .value(description)
            .build();
    }
} 