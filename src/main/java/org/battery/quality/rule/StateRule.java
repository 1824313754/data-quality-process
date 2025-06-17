package org.battery.quality.rule;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;

import java.util.List;

/**
 * 需要维护状态的规则接口
 */
public interface StateRule extends Rule {
    /**
     * 检查当前数据和上一条数据
     * @param current 当前数据
     * @param previous 前一条数据 (可能为null)
     * @return 质量问题列表
     */
    List<Issue> checkState(Gb32960Data current, Gb32960Data previous);
} 