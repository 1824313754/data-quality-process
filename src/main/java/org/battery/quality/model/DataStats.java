package org.battery.quality.model;

import lombok.Data;
import lombok.Builder;

import java.io.Serializable;

/**
 * 数据统计信息模型类
 * 用于记录每个vin每天每小时的实时数据量
 * 使用String类型存储日期时间，避免序列化问题
 */
@Data
@Builder
public class DataStats implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String vin;                // 车辆VIN码
    private String dayOfYear;          // 数据日期，格式：yyyy-MM-dd
    private Integer hour;              // 小时(0-23)
    private String vehicleFactory;     // 车厂
    private Long normalDataCount;      // 正常数据条数
    private Long abnormalDataCount;    // 异常数据条数
    private Long dataCount;            // 总数据条数
    private String time;               // 数据时间，格式：yyyy-MM-dd HH:mm:ss
    private String lastUpdateTime;     // 最近更新时间，格式：yyyy-MM-dd HH:mm:ss
}