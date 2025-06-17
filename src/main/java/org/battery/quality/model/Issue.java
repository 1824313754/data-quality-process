package org.battery.quality.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据质量异常模型类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    // 异常ID
    private String id;
    
    // 异常编码
    private int code;
    
    // 车辆VIN码
    private String vin;
    
    // 异常描述
    private String description;
    
    // 异常值
    private String value;
    
    // 异常类型
    private String type;
    
    // 异常发生时间
    private Long timestamp;
} 