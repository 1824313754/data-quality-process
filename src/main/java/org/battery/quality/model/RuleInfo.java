package org.battery.quality.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 规则信息类，用于在Flink节点间传输规则信息，避免直接序列化规则对象
 * 解决动态编译生成类的序列化问题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "sourceCode")
public class RuleInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;                // 规则ID
    private String name;              // 规则名称
    private String description;       // 规则描述
    private String category;          // 规则分类
    private int ruleCode;             // 异常编码
    private int priority;             // 规则优先级
    private String sourceCode;        // 规则源代码
    private String enabledFactories;  // 启用的车厂ID列表，逗号分隔，0表示所有车厂
    private Timestamp createTime;     // 创建时间
    private Timestamp updateTime;     // 更新时间
    private int status;               // 规则状态，1表示启用，0表示禁用
    
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
} 