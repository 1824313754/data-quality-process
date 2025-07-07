package org.battery.quality.rule.template;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.rule.StateRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * 抽象状态规则类
 * 扩展抽象规则类并实现状态规则接口
 */
public abstract class AbstractStateRule extends AbstractRule implements StateRule {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStateRule.class);
    
    @Override
    public final List<Issue> checkState(Gb32960Data current, Gb32960Data previous) {
        // 如果当前数据为空，无法检查
        if (current == null) {
            LOGGER.warn("规则 {} 状态检查时收到null当前数据", getType());
            return Collections.emptyList();
        }
        
        try {
            // 前置条件检查
            if (!preCheckState(current, previous)) {
                return Collections.emptyList();
            }
            
            // 执行具体的状态检查逻辑
            return doCheckState(current, previous);
        } catch (Exception e) {
            LOGGER.error("规则 {} 执行状态检查异常", getType(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 状态前置条件检查
     * 子类可以重写此方法进行状态数据有效性检查
     * 
     * @param current 当前数据
     * @param previous 前一条数据
     * @return 是否继续检查，返回false则跳过后续检查
     */
    protected boolean preCheckState(Gb32960Data current, Gb32960Data previous) {
        return true;
    }
    
    /**
     * 执行具体的状态检查逻辑
     * 子类必须实现此方法提供具体的状态规则检查逻辑
     * 
     * @param current 当前数据
     * @param previous 前一条数据
     * @return 检查结果，包含发现的问题
     */
    protected abstract List<Issue> doCheckState(Gb32960Data current, Gb32960Data previous);
    
    @Override
    protected List<Issue> doCheck(Gb32960Data data) {
        // 单条数据检查默认不产生异常
        // 状态检查通过checkState方法实现
        return Collections.emptyList();
    }
} 