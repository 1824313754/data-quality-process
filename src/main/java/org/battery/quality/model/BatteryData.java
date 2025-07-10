package org.battery.quality.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 电池数据模型类
 * 重构后统一命名，保持数据结构一致性
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatteryData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 基本车辆信息
    private String vin;                     // 车辆VIN码
    private String vehicleFactory;          // 车辆厂商代码
    private String time;                    // 数据时间，格式为"yyyy-MM-dd HH:mm:ss"
    
    // 车辆状态信息
    private Integer vehicleStatus;          // 车辆状态
    private Integer chargeStatus;           // 充电状态
    private Integer speed;                  // 车速
    private Integer mileage;                // 里程
    
    // 电池信息
    private Integer totalVoltage;           // 总电压
    private Integer totalCurrent;           // 总电流
    private Integer soc;                    // 电池SOC
    private Integer dcStatus;               // DC-DC状态
    private Integer gears;                  // 档位
    private Integer insulationResistance;   // 绝缘电阻
    private Integer operationMode;          // 运行模式
    
    // 电池包信息
    private Integer batteryCount;           // 电池包数量
    private Integer batteryNumber;          // 电池编号
    private Integer cellCount;              // 电池单体数量
    
    // 电压信息
    private Integer maxVoltagebatteryNum;   // 最高电压电池序号
    private Integer maxVoltageSystemNum;    // 最高电压系统号
    private Integer batteryMaxVoltage;      // 电池最高电压
    private Integer minVoltagebatteryNum;   // 最低电压电池序号
    private Integer minVoltageSystemNum;    // 最低电压系统号
    private Integer batteryMinVoltage;      // 电池最低电压
    
    // 温度信息
    private Integer maxTemperature;         // 最高温度
    private Integer maxTemperatureNum;      // 最高温度探针序号
    private Integer maxTemperatureSystemNum; // 最高温度系统号
    private Integer minTemperature;         // 最低温度
    private Integer minTemperatureNum;      // 最低温度探针序号
    private Integer minTemperatureSystemNum; // 最低温度系统号
    
    // 子系统信息
    private Integer subsystemVoltageCount;   // 子系统电压数量
    private Integer subsystemVoltageDataNum; // 子系统电压数据编号
    private Integer subsystemTemperatureCount; // 子系统温度数量
    private Integer subsystemTemperatureDataNum; // 子系统温度数据编号
    private Integer temperatureProbeCount;   // 温度探针数量
    
    // 位置信息
    private Long longitude;                 // 经度
    private Long latitude;                  // 纬度
    
    // 列表数据
    private List<Integer> cellVoltages;     // 电池单体电压列表
    private List<Integer> probeTemperatures; // 温度探针列表
    private List<Integer> deviceFailuresCodes; // 设备故障码列表
    private List<Integer> driveMotorFailuresCodes; // 驱动电机故障码列表
    
    // 其他信息
    private String customField;             // 自定义字段
    private String ctime;                   // 处理时间，格式为"yyyy-MM-dd HH:mm:ss"

    // 原始时间字段（用于时间有效性检查）
    private Integer rawYear;                // 原始年份字段
    private Integer rawMonth;               // 原始月份字段
    private Integer rawDay;                 // 原始日期字段
    private Integer rawHours;               // 原始小时字段
    private Integer rawMinutes;             // 原始分钟字段
    private Integer rawSeconds;             // 原始秒数字段
} 