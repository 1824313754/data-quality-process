package org.battery.quality.rule;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import java.util.Collections;
import java.util.List;

/**
 * 基础状态规则实现
 */
public abstract class BaseStateRule extends BaseRule implements StateRule {
    
    @Override
    public List<Issue> check(Gb32960Data data) {
        // 单条数据检查默认不产生异常
        // 状态检查通过checkState方法实现
        return Collections.emptyList();
    }
} 