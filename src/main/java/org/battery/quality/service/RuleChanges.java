package org.battery.quality.service;

import org.battery.quality.model.RuleInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则变更信息
 * 用于记录规则的增删改变更
 */
public class RuleChanges {
    
    // 删除的规则ID列表
    private final List<String> deletedRules = new ArrayList<>();
    
    // 新增或修改的规则列表
    private final List<RuleInfo> addedOrModifiedRules = new ArrayList<>();
    
    /**
     * 添加删除的规则
     */
    public void addDeletedRule(String ruleId) {
        deletedRules.add(ruleId);
    }
    
    /**
     * 添加新增或修改的规则
     */
    public void addAddedOrModifiedRule(RuleInfo ruleInfo) {
        addedOrModifiedRules.add(ruleInfo);
    }
    
    /**
     * 获取删除的规则列表
     */
    public List<String> getDeletedRules() {
        return deletedRules;
    }
    
    /**
     * 获取新增或修改的规则列表
     */
    public List<RuleInfo> getAddedOrModifiedRules() {
        return addedOrModifiedRules;
    }
    
    /**
     * 是否有变更
     */
    public boolean hasChanges() {
        return !deletedRules.isEmpty() || !addedOrModifiedRules.isEmpty();
    }
    
    /**
     * 获取变更总数
     */
    public int getTotalChanges() {
        return deletedRules.size() + addedOrModifiedRules.size();
    }
    
    @Override
    public String toString() {
        return String.format("RuleChanges{删除:%d, 新增/修改:%d}", 
                deletedRules.size(), addedOrModifiedRules.size());
    }
}
