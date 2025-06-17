package org.battery.quality.rule;

import org.battery.quality.model.Gb32960Data;
import org.battery.quality.model.Issue;
import org.battery.quality.model.RuleType;

import java.util.List;

/**
 * 数据质量规则接口
 */
public interface Rule {
    /**
     * 检测单条数据
     * @param data 车辆数据
     * @return 质量问题列表，如果没有问题则返回空列表
     */
    List<Issue> check(Gb32960Data data);
    
    /**
     * 获取规则类型
     * @return 规则类型
     */
    String getType();
    
    /**
     * 获取规则异常编码
     * @return 异常编码
     */
    int getCode();
    
    /**
     * 获取规则描述
     * @return 规则描述
     */
    String getDescription();
    
    /**
     * 获取规则分类
     * @return 规则分类
     */
    RuleType getCategory();
    
    /**
     * 获取规则优先级
     * @return 规则优先级
     */
    int getPriority();
} 