# 电池数据质量分析系统

## 项目简介

电池数据质量分析系统是一个基于Apache Flink的实时数据处理平台，专门用于电动汽车电池数据的质量检测和分析。系统实时接收来自Kafka的电池数据，通过一系列预设和自定义的质量规则进行检测，识别出可能存在的数据异常，并将结果保存到Doris数据库以供后续分析和可视化。

## 系统特点

- **实时处理**：基于Flink流处理框架，支持毫秒级的数据处理延迟
- **多维度质量检测**：支持完整性、有效性、一致性、时效性四大类规则
- **可扩展规则引擎**：基于插件化设计，支持动态加载和更新规则
- **车厂定制化**：支持根据不同车厂定制不同的规则集
- **数据统计**：按车辆VIN码、日期、小时等维度进行数据统计
- **高性能存储**：使用Doris数据库存储处理结果，支持高性能查询和分析

## 系统架构

### 整体架构
```
Kafka数据源 → Flink处理引擎(规则检测) → Doris存储 → 分析平台
```

### 模块结构
```
org.battery.quality
├── config/           # 配置相关
├── model/            # 数据模型
├── processor/        # 数据处理器
├── rule/             # 规则定义与实现
│   ├── annotation/   # 规则注解
│   ├── impl/         # 规则实现
│     ├── completeness/  # 完整性规则
│     ├── consistency/   # 一致性规则
│     ├── timeliness/    # 时效性规则
│     └── validity/      # 有效性规则
├── service/          # 服务层
├── sink/             # 输出模块
├── source/           # 数据源模块
├── transformer/      # 数据转换器
└── util/             # 工具类
```

## 核心功能

### 1. 数据质量检测
- **完整性检测**：检查数据字段是否缺失(例如电池电压、温度数据等)
- **有效性检测**：检查数据值是否在合理范围内(例如SOC、电压、电流等)
- **一致性检测**：检查数据内部或与历史数据是否一致(例如电池单体数量)
- **时效性检测**：检查数据的时间戳是否满足要求(例如数据延迟、超前)

### 2. 数据统计分析
- 按车辆VIN码统计正常/异常数据量
- 按日期、小时维度统计数据分布
- 按车厂统计数据质量情况

### 3. 灵活配置
- 支持通过配置文件设置Kafka、Flink、Doris等参数
- 支持通过数据库动态更新规则

## 技术栈

- **Apache Flink 1.13**：分布式流处理框架
- **Apache Kafka**：消息队列，数据源
- **Apache Doris**：分析型数据库，结果存储
- **MySQL**：规则存储和管理
- **Java 1.8**：开发语言

## 设计模式应用

系统在设计和实现过程中应用了多种设计模式：

- **单例模式**：用于配置管理等全局对象
- **工厂模式**：用于创建不同类型的对象，如规则、数据源等
- **策略模式**：用于实现不同的规则策略
- **模板方法模式**：用于规则基类设计，提供通用处理流程
- **建造者模式**：用于构建复杂对象，如配置对象、数据对象等
- **责任链模式**：用于规则链式处理

## 核心流程

1. **数据接入**：从Kafka读取电池数据
2. **数据预处理**：解析、转换和规范化数据
3. **规则处理**：应用各类规则检测数据质量
4. **结果输出**：将处理结果和统计数据写入Doris
5. **异常监控**：监控处理过程中的异常情况

## 快速开始

### 环境要求
- Java 1.8+
- Maven 3.6+
- Kafka 2.4+
- Doris 1.0+
- MySQL 5.7+

### 编译打包
```bash
mvn clean package
```

### 配置说明
配置文件路径：`src/main/resources/application.yml`

```yaml
# Kafka配置
kafka:
  bootstrapServers: localhost:9092
  topic: battery-data
  groupId: data-quality-group

# 处理配置
process:
  parallelism: 4
  checkpointInterval: 60000  # 毫秒

# MySQL配置
mysql:
  url: jdbc:mysql://localhost:3306/battery_quality
  username: root
  password: password
  
# Doris配置
doris:
  conn: localhost:8030
  user: root
  passwd: 
  database: battery_data
  table: gb32960_data_with_issues
```

### 启动命令
```bash
# 本地模式
flink run -c org.battery.quality.DataQualityApplication target/data-quality-process-1.0-SNAPSHOT.jar

# 集群模式
flink run -m yarn-cluster -yn 2 -yjm 1024 -ytm 4096 \
  -c org.battery.quality.DataQualityApplication \
  target/data-quality-process-1.0-SNAPSHOT.jar
```

## 数据模型

### 输入数据模型(BatteryData)
```java
public class BatteryData {
    private String vin;                     // 车辆VIN码
    private String vehicleFactory;          // 车辆厂商代码
    private String time;                    // 数据时间
    private Integer vehicleStatus;          // 车辆状态
    private Integer chargeStatus;           // 充电状态
    private Integer speed;                  // 车速
    private Integer soc;                    // 电池SOC
    // ... 其他字段
}
```

### 输出数据模型
```java
public class ProcessedData {
    private BatteryData data;               // 原始电池数据
    private List<QualityIssue> issues;      // 发现的质量问题
}

public class QualityIssue {
    private int code;                       // 问题编码
    private String value;                   // 问题值
    private String type;                    // 问题类型
    private String description;             // 问题描述
    private int severity;                   // 严重程度
}
```

### 统计数据模型
```java
public class DataStats {
    private String vin;                     // 车辆VIN码
    private String dayOfYear;               // 数据日期
    private Integer hour;                   // 小时(0-23)
    private String vehicleFactory;          // 车厂
    private Long normalDataCount;           // 正常数据条数
    private Long abnormalDataCount;         // 异常数据条数
    private Long dataCount;                 // 总数据条数
    // ... 其他字段
}
```

## 规则开发指南

### 新增规则步骤
1. 在`rule.impl`包中创建规则类
2. 继承`AbstractRule`或`AbstractStateRule`基类
3. 添加`@RuleDefinition`注解
4. 实现具体的检测逻辑

### 规则示例
```java
@RuleDefinition(
    type = "SOC_VALIDITY",
    code = 1002,
    description = "SOC值无效",
    category = RuleCategory.VALIDITY
)
public class SocValidityRule extends AbstractRule {
    
    @Override
    protected List<QualityIssue> doCheck(BatteryData data) {
        if (data.getSoc() == null) {
            return noIssue();
        }
        
        // SOC有效范围: 0-100
        if (data.getSoc() < 0 || data.getSoc() > 100) {
            return singleIssue(
                data, 
                "SOC值 " + data.getSoc() + " 超出有效范围[0,100]"
            );
        }
        
        return noIssue();
    }
}
```

## 性能优化

- 使用键控状态(Keyed State)管理每辆车的历史数据
- 采用侧输出流(Side Output)分离主流和统计流
- 批量写入Doris提高吞吐量
- 动态分区优化Doris存储性能

## 许可证

本项目采用Apache 2.0许可证
