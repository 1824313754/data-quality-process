package org.battery.quality.rule.impl.validity;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;
import org.battery.quality.rule.AbstractRule;
import org.battery.quality.rule.RuleCategory;
import org.battery.quality.rule.annotation.RuleDefinition;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间字段有效性检查规则
 * 检查时间字符串是否有效且可以构成一个合法的时间
 */
@RuleDefinition(
        type = "COLLECTION_TIME_VALIDITY",
        code = 1014,
        description = "采集时间无效",
        category = RuleCategory.VALIDITY,
        priority = 20
)
public class DateTimeFieldsValidityRule extends AbstractRule {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<QualityIssue> check(BatteryData data) {
        List<QualityIssue> issues = new ArrayList<>();
        String timeStr = data.getTime();
        
        if (timeStr == null || timeStr.trim().isEmpty()) {
            issues.add(QualityIssue.builder()
                .code(getCode())
                .value("时间字符串为空")
                .build());
            
            // 使用当前时间作为ctime
            data.setCtime(getCurrentTimeAsString());
            return issues;
        }
        
        try {
            // 尝试解析时间字符串
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, FORMATTER);
            
            // 设置ctime为原始时间字符串
            data.setCtime(timeStr);
            
        } catch (DateTimeParseException e) {
            issues.add(QualityIssue.builder()
                .code(getCode())
                .value("无法解析时间字符串: " + timeStr + ", 错误: " + e.getMessage())
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
        return LocalDateTime.now().format(FORMATTER);
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
    protected QualityIssue createIssue(BatteryData data, String description) {
        return QualityIssue.builder()
            .code(getCode())
            .value(description)
            .build();
    }
} 