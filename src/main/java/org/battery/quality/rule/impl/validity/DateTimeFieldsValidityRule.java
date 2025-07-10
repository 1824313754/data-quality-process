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
 * 采集时间字段有效性检查规则
 * 检查ctime字段是否有效且可以构成一个合法的时间
 * 注意：此规则专门验证ctime字段，不修改数据
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
        String ctimeStr = data.getCtime();

        // 检查ctime字段是否为空
        if (ctimeStr == null || ctimeStr.trim().isEmpty()) {
            issues.add(createIssue(data, "采集时间字段为空"));
            return issues;
        }

        try {
            // 尝试解析采集时间字符串
            LocalDateTime dateTime = LocalDateTime.parse(ctimeStr, FORMATTER);

            // 验证时间的合理性
            if (!isValidDateTime(dateTime)) {
                issues.add(createIssue(data, "采集时间不合理: " + ctimeStr));
            }

        } catch (DateTimeParseException e) {
            issues.add(createIssue(data, "无法解析采集时间: " + ctimeStr + ", 错误: " + e.getMessage()));
        }

        return issues;
    }

    /**
     * 验证时间是否合理
     * @param dateTime 解析后的时间
     * @return 是否合理
     */
    private boolean isValidDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minValidTime = now.minusYears(10); // 10年前
        LocalDateTime maxValidTime = now.plusDays(1);    // 1天后

        return dateTime.isAfter(minValidTime) && dateTime.isBefore(maxValidTime);
    }
    

} 