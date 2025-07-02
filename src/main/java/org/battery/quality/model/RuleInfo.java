package org.battery.quality.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 规则信息类，用于在Flink节点间传输规则信息，避免直接序列化规则对象
 * 解决动态编译生成类的序列化问题
 */
public class RuleInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;                // 规则ID
    private String name;              // 规则类名
    private String sourceCode;        // 规则源代码
    private String enabledFactories;  // 启用的车厂ID列表，逗号分隔，0表示所有车厂
    private Timestamp updateTime;     // 更新时间
    
    public RuleInfo() {
    }
    
    public RuleInfo(String id, String name, String sourceCode, String enabledFactories) {
        this.id = id;
        this.name = name;
        this.sourceCode = sourceCode;
        this.enabledFactories = enabledFactories;
    }
    
    public RuleInfo(String id, String name, String sourceCode, String enabledFactories, Timestamp updateTime) {
        this.id = id;
        this.name = name;
        this.sourceCode = sourceCode;
        this.enabledFactories = enabledFactories;
        this.updateTime = updateTime;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSourceCode() {
        return sourceCode;
    }
    
    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
    
    public String getEnabledFactories() {
        return enabledFactories;
    }
    
    public void setEnabledFactories(String enabledFactories) {
        this.enabledFactories = enabledFactories;
    }
    
    public Timestamp getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
    
    /**
     * 获取规则缓存键（规则ID:更新时间）
     * @return 缓存键
     */
    public String getCacheKey() {
        if (updateTime != null) {
            return id + ":" + updateTime.getTime();
        } else {
            return id;
        }
    }
    
    /**
     * 判断规则是否适用于指定车厂
     * @param factoryId 车厂ID
     * @return 是否适用
     */
    public boolean isEnabledForFactory(String factoryId) {
        if (enabledFactories == null || enabledFactories.isEmpty()) {
            return false;
        }
        
        // 0表示适用于所有车厂
        if (enabledFactories.equals("0")) {
            return true;
        }
        
        // 检查是否包含指定车厂ID
        for (String id : enabledFactories.split(",")) {
            if (id.trim().equals(factoryId)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "RuleInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", enabledFactories='" + enabledFactories + '\'' +
                ", updateTime='" + (updateTime != null ? updateTime : "null") + '\'' +
                '}';
    }
} 