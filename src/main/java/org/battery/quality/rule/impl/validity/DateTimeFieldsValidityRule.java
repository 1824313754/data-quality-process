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
 * 检查原始JSON数据中的年月日时分秒字段是否合法
 * 验证字段：hours, seconds, month, year, minutes, day
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

        // 检查原始时间字段是否存在
        Integer rawYear = data.getRawYear();
        Integer rawMonth = data.getRawMonth();
        Integer rawDay = data.getRawDay();
        Integer rawHours = data.getRawHours();
        Integer rawMinutes = data.getRawMinutes();
        Integer rawSeconds = data.getRawSeconds();

        // 检查字段是否为空
        if (rawYear == null || rawMonth == null || rawDay == null ||
            rawHours == null || rawMinutes == null || rawSeconds == null) {
            issues.add(createIssue(data, "年月日时分秒字段存在空值"));
            return issues;
        }

        // 检查年份有效性 (假设合理范围是2000-2099)
        int fullYear = 2000 + rawYear;
        if (rawYear < 0 || rawYear > 99) {
            issues.add(createIssue(data, String.format("年份无效: %d (应为0-99)", rawYear)));
        }

        // 检查月份有效性 (1-12)
        if (rawMonth < 1 || rawMonth > 12) {
            issues.add(createIssue(data, String.format("月份无效: %d (应为1-12)", rawMonth)));
        }

        // 检查日期有效性 (1-31，需要考虑月份)
        if (rawDay < 1 || rawDay > 31) {
            issues.add(createIssue(data, String.format("日期无效: %d (应为1-31)", rawDay)));
        } else if (rawMonth >= 1 && rawMonth <= 12) {
            // 进一步检查每月的天数
            int maxDaysInMonth = getMaxDaysInMonth(fullYear, rawMonth);
            if (rawDay > maxDaysInMonth) {
                issues.add(createIssue(data, String.format("日期无效: %d月不能有%d天", rawMonth, rawDay)));
            }
        }

        // 检查小时有效性 (0-23)
        if (rawHours < 0 || rawHours > 23) {
            issues.add(createIssue(data, String.format("小时无效: %d (应为0-23)", rawHours)));
        }

        // 检查分钟有效性 (0-59)
        if (rawMinutes < 0 || rawMinutes > 59) {
            issues.add(createIssue(data, String.format("分钟无效: %d (应为0-59)", rawMinutes)));
        }

        // 检查秒数有效性 (0-59)
        if (rawSeconds < 0 || rawSeconds > 59) {
            issues.add(createIssue(data, String.format("秒数无效: %d (应为0-59)", rawSeconds)));
        }

        // 如果所有字段都有效，尝试构造时间验证整体有效性
        if (issues.isEmpty()) {
            try {
                LocalDateTime.of(fullYear, rawMonth, rawDay, rawHours, rawMinutes, rawSeconds);
            } catch (Exception e) {
                issues.add(createIssue(data, String.format("时间组合无效: %d-%02d-%02d %02d:%02d:%02d",
                        fullYear, rawMonth, rawDay, rawHours, rawMinutes, rawSeconds)));
            }
        }

        return issues;
    }

    /**
     * 获取指定年月的最大天数
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 该月的最大天数
     */
    private int getMaxDaysInMonth(int year, int month) {
        switch (month) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                return 31;
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                // 检查闰年
                return isLeapYear(year) ? 29 : 28;
            default:
                return 31; // 默认值，不应该到达这里
        }
    }

    /**
     * 判断是否为闰年
     * @param year 年份
     * @return 是否为闰年
     */
    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
    

} 