package org.battery.quality.rule;

import org.battery.quality.model.BatteryData;
import org.battery.quality.model.QualityIssue;

import java.util.List;

/**
 * 抽象状态规则基类
 * 实现IStateRule接口的通用方法
 */
public abstract class AbstractStateRule extends AbstractRule implements IStateRule {
    
    @Override
    public List<QualityIssue> check(BatteryData data) {
        // 默认实现返回空列表，状态规则主要依靠checkState方法
        return noIssue();
    }
} 