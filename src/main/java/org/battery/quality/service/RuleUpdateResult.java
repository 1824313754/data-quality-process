package org.battery.quality.service;

/**
 * 规则更新结果
 * 记录规则更新的统计信息
 */
public class RuleUpdateResult {
    
    public int addedCount = 0;      // 新增规则数量
    public int modifiedCount = 0;   // 修改规则数量
    public int deletedCount = 0;    // 删除规则数量
    public int errorCount = 0;      // 错误数量
    
    /**
     * 获取总变更数量
     */
    public int getTotalChanges() {
        return addedCount + modifiedCount + deletedCount;
    }
    
    /**
     * 是否有变更
     */
    public boolean hasChanges() {
        return getTotalChanges() > 0;
    }
    
    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    @Override
    public String toString() {
        return String.format("RuleUpdateResult{新增:%d, 修改:%d, 删除:%d, 错误:%d}", 
                addedCount, modifiedCount, deletedCount, errorCount);
    }
}
