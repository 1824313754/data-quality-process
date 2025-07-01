# 电池数据质量分析系统

## 系统概述
电池数据质量分析系统是一个基于Apache Flink的流处理应用，用于实时接收、处理和分析GB/T 32960电动汽车国标数据。系统主要功能是对接收到的车辆电池数据进行质量检查，发现数据中的问题并将结果输出到Doris数据库中进行存储和后续分析。

## 系统架构

系统采用典型的流处理架构，主要包含以下组件：

1. **数据源**：Kafka消息队列，接收GB/T 32960格式的车辆数据
2. **规则源**：MySQL数据库，存储和管理数据质量检查规则
3. **处理引擎**：Apache Flink，实现流式数据处理
4. **数据存储**：Apache Doris，存储处理结果和质量问题

系统整体架构如下图所示：

```
Kafka(GB32960数据) --> Flink处理引擎 --> Doris存储系统
                   ^
                   |
MySQL(规则管理) -----+
```

## 核心组件说明

### 1. 数据模型

系统主要处理两类数据：

- **Gb32960Data**: 国标32960格式的电池数据实体类
  - `time`: 数据采集时间，String类型，格式为"yyyy-MM-dd HH:mm:ss"，在规则计算时需要转换为时间戳
  - `ctime`: 数据处理时间，String类型，格式为"yyyy-MM-dd HH:mm:ss"，在规则计算时需要转换为时间戳
- **Gb32960DataWithIssues**: 处理后的数据，包含原始数据和检测到的质量问题
- **Issue**: 表示数据质量异常的详细信息

### 2. 数据质量规则

系统支持多种类型的数据质量规则检查：

- **完整性规则 (Completeness)**: 检查数据是否完整，例如缺少必要的字段
- **一致性规则 (Consistency)**: 检查数据内部是否一致，例如数组长度与声明的计数是否一致
- **时效性规则 (Timeliness)**: 检查数据的时间属性，例如数据延迟、时间戳单调性
- **有效性规则 (Validity)**: 检查数据值是否在合法范围内，例如SOC、电压、温度等

规则通过注解方式定义，支持动态加载和配置。

### 3. 处理流程

1. 从Kafka读取GB/T 32960数据
2. 从MySQL读取并广播规则配置
3. 对数据应用质量规则检查
4. 标记质量问题并输出结果到Doris

### 4. Sink抽象

系统使用Sink接口抽象数据输出逻辑，通过SinkFactory创建适当的Sink实现。目前支持以下Sink类型：

- **DorisSink**: 将数据写入Doris数据库
- **PrintSink**: 将数据打印到控制台，支持简洁和详细两种模式
- **MultipleSink**: 将数据同时发送到多个Sink（例如同时写入数据库并打印到控制台）

Sink接口设计为返回SinkFunction，便于与Flink的流处理API集成。可通过配置参数`sink.type`选择不同的Sink类型：
- `doris`: 使用DorisSink (默认)
- `print`: 使用PrintSink
- `both`: 同时使用DorisSink和PrintSink

对于PrintSink，可通过以下参数进行配置：
- `print.identifier`: 输出标识符，默认为"质量检查结果"
- `print.verbose`: 是否打印详细信息，默认为false

## 配置说明

系统配置通过application.yml文件提供，主要包含：

- **Kafka配置**: 连接信息、消费组、主题等
- **处理配置**: 并行度、状态保留时间、检查点间隔等
- **MySQL配置**: 数据库连接信息、连接池配置等
- **Doris配置**: 连接信息、批处理大小、批处理间隔等
- **Sink配置**: Sink类型及其特定配置

## 部署与运行

### 环境要求

- Java 8+
- Apache Flink 1.13+
- Apache Kafka
- MySQL 5.7+
- Apache Doris

### 编译部署

```bash
# 编译
mvn clean package

# 运行
flink run -c org.battery.DataQualityApplication target/data-quality-process-1.0-SNAPSHOT.jar
```

## Doris建表语句

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS battery_data;

-- 创建单一的数据表，包含原始数据和质量问题信息
CREATE TABLE battery_ods.error_data (
                                        vin varchar(255),
                                        ctime datetime,
                                        time datetime,
                                        vehicleFactory varchar(255),
                                        vehicleStatus INT,
                                        chargeStatus INT,
                                        speed INT,
                                        mileage INT,
                                        totalVoltage INT,
                                        totalCurrent INT,
                                        soc INT,
                                        dcStatus INT,
                                        gears INT,
                                        insulationResistance INT,
                                        operationMode INT,
                                        batteryCount INT,
                                        batteryNumber INT,
                                        cellCount INT,
                                        maxVoltagebatteryNum INT,
                                        maxVoltageSystemNum INT,
                                        batteryMaxVoltage INT,
                                        minVoltagebatteryNum INT,
                                        minVoltageSystemNum INT,
                                        batteryMinVoltage INT,
                                        maxTemperature INT,
                                        maxTemperatureNum INT,
                                        maxTemperatureSystemNum INT,
                                        minTemperature INT,
                                        minTemperatureNum INT,
                                        minTemperatureSystemNum INT,
                                        subsystemVoltageCount INT,
                                        subsystemVoltageDataNum INT,
                                        subsystemTemperatureCount INT,
                                        subsystemTemperatureDataNum INT,
                                        temperatureProbeCount INT,
                                        longitude BIGINT,
                                        latitude BIGINT,
                                        customField String,
                                        cellVoltages array<int>,
                                        probeTemperatures array<int>,
                                        deviceFailuresCodes array<int>,
                                        driveMotorFailuresCodes array<int>,
                                        issues ARRAY<String> , -- JSON格式的质量问题列表
                                        issues_count INT -- 数据质量问题总数
)
    DUPLICATE KEY(vin, ctime)
PARTITION BY RANGE(time)()
DISTRIBUTED BY HASH(`vin`) BUCKETS AUTO
PROPERTIES (
"replication_allocation" = "tag.location.offline: 1",
"min_load_replica_num" = "-1",
"bloom_filter_columns" = "ctime",
"is_being_synced" = "false",
"dynamic_partition.enable" = "true",
"dynamic_partition.time_unit" = "DAY",
"dynamic_partition.time_zone" = "Asia/Shanghai",
"dynamic_partition.start" = "-90",
"dynamic_partition.end" = "2",
"dynamic_partition.prefix" = "p",
"dynamic_partition.replication_allocation" = "tag.location.offline: 1",
"dynamic_partition.buckets" = "10",
"dynamic_partition.create_history_partition" = "false",
"dynamic_partition.history_partition_num" = "-1",
"dynamic_partition.reserved_history_periods" = "NULL",
"dynamic_partition.storage_policy" = "",
"storage_medium" = "hdd",
"storage_format" = "V2",
"inverted_index_storage_format" = "V1",
"compression" = "ZSTD",
"light_schema_change" = "true",
"disable_auto_compaction" = "false",
"enable_single_replica_compaction" = "false",
"group_commit_interval_ms" = "10000",
"group_commit_data_bytes" = "134217728"
);
```

## 最近更新

### 规则模块重构 (2023-08-01)

为了提高代码质量和可维护性，系统对规则模块进行了全面重构，主要采用了多种设计模式：

1. **工厂模式**：
   - 引入`RuleFactory`接口和`RuleFactoryRegistry`类，负责创建规则实例
   - 支持多种规则创建方式，包括动态编译

2. **策略模式**：
   - 使用`RuleExecutionStrategy`接口和实现类处理不同类型规则的执行逻辑
   - 通过`StrategySelector`类动态选择合适的执行策略

3. **责任链模式**：
   - 新增`RuleChain`接口和`DefaultRuleChain`实现，负责组织规则执行流程
   - 支持按规则类型分组执行，并使用`RuleChainBuilder`构建规则链

4. **观察者模式**：
   - 使用`RuleUpdateObserver`和`RuleUpdateSubject`接口处理规则更新事件
   - 通过`RuleUpdateManager`集中管理规则更新通知

5. **模板方法模式**：
   - 重构`AbstractRule`和`AbstractStateRule`类，定义规则执行的通用流程
   - 子类只需实现特定的检查逻辑，减少代码重复

6. **适配器模式**：
   - 引入`RuleAdapter`接口和实现类，处理不同类型对象到规则的转换
   - 通过`AdapterRegistry`管理所有适配器

7. **单例模式**：
   - 对`RuleManager`、`RuleFactoryRegistry`等核心组件使用单例模式
   - 确保全局唯一实例，避免资源浪费

8. **服务层模式**：
   - 引入`RuleService`接口和`DefaultRuleService`实现，提供规则相关的业务逻辑
   - 将数据库操作封装在服务层，与规则执行逻辑分离

这次重构的主要优点：

- **更高的可扩展性**：可以方便地添加新的规则类型和执行策略
- **更好的可维护性**：各组件职责明确，降低了耦合度
- **更强的灵活性**：规则可以动态更新，不需要重启系统
- **更高的代码质量**：遵循阿里巴巴Java编程规范，提高了代码可读性和稳定性

### 时间字段格式变更 (2023-07-01)

为提高数据可读性和使用便利性，系统对时间字段进行了以下更新：

1. **时间字段格式变更**：
   - 将 `time` 和 `ctime` 字段从时间戳(Long)改为String类型，格式为"yyyy-MM-dd HH:mm:ss"
   - 这些字段在存储和展示时使用字符串格式，便于直观查看

2. **规则适配**：
   - 更新了所有涉及时间字段的规则，使每个规则内部负责解析字符串时间为时间戳
   - 所有规则类中使用SimpleDateFormat解析字符串格式的时间，进行计算后再生成结果
   - 增加了异常处理，提高系统稳定性

3. **好处**：
   - 提高了数据的可读性
   - 允许以更自然的方式查询和过滤数据
   - 保持了时间计算的准确性
此更新使得数据在显示和存储时更加直观，便于阅读和理解，同时通过在规则内部解析时间戳的方式保证了计算的准确性。
