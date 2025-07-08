package org.battery.quality.rule;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;

import java.util.List;

/**
 * 有状态规则接口
 * 需要跟踪前一条数据的规则实现此接口
 */
public interface IStateRule extends IRule {
    /**
     * 检查当前数据和前一条数据
     * 
     * @param current 当前数据
     * @param previous 前一条数据（可能为null）
     * @return 质量问题列表
     */
    List<QualityIssue> checkState(BatteryData current, BatteryData previous);
} 