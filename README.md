# 电池数据质量分析系统

## 1. 项目介绍

本系统是一个基于Flink的电池数据质量分析平台，用于实时检测和处理电池相关数据的质量问题。系统从Kafka获取数据，通过各种规则检测数据质量，并将结果输出到Doris或其他目标系统，帮助用户及时发现并解决数据质量问题。

### 主要功能
- **数据质量检测**：支持多种质量规则类型，如有效性、完整性、一致性、时效性等
- **规则动态加载**：支持从数据库动态加载和更新规则
- **可扩展的规则引擎**：基于设计模式，易于添加新规则
- **多种输出方式**：支持将结果输出到Doris、MySQL或控制台等

## 2. 项目结构

```
data-quality-process/
├── src/main/java/org/battery/
│   ├── quality/
│   │   ├── config/        # 配置相关
│   │   │   ├── KafkaConfig.java
│   │   │   ├── ProcessConfig.java
│   │   │   ├── MySQLConfig.java
│   │   │   ├── SinkConfig.java
│   │   │   └── DorisConfig.java
│   │   ├── core/          # 核心应用
│   │   ├── model/         # 数据模型
│   │   ├── processor/     # 数据处理器
│   │   ├── rule/          # 规则定义与实现
│   │   │   ├── annotation/  # 规则注解
│   │   │   ├── impl/        # 具体规则实现
│   │   ├── service/       # 服务层
│   │   ├── sink/          # 输出模块
│   │   ├── source/        # 数据源模块
│   │   └── util/          # 工具类
│   └── DataQualityApplication.java  # 主入口类
├── src/main/resources/
│   ├── application.yml    # 应用配置文件
│   ├── db/schema.sql      # 数据库脚本
│   └── logback.xml        # 日志配置
```

## 3. 设计模式应用

### 单例模式
用于确保全局只有一个实例，如配置管理器：
```java
public class ConfigManager {
    private static ConfigManager instance;
    
    private ConfigManager() { /* 私有构造函数 */ }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
}
```

### 工厂模式
用于创建对象，解耦对象的创建和使用：
```java
public class SinkFactory {
    public static Sink createSink(String type, Map<String, Object> config) {
        switch (type) {
            case "doris":
                return new FlinkDorisSink(config);
            case "print":
                return new PrintSink(config);
            default:
                throw new IllegalArgumentException("不支持的Sink类型: " + type);
        }
    }
}
```

### 策略模式
用于实现不同算法或策略，如不同的规则实现：
```java
@RuleDefinition(type = "SOC_VALIDITY", code = 1002)
public class SocValidityRule extends AbstractRule {
    @Override
    public List<QualityIssue> check(BatteryData data) {
        // SOC有效性检查策略
    }
}
```

### 模板方法模式
用于定义算法骨架，子类可以重写特定步骤：
```java
public abstract class AbstractRule implements IRule {
    @Override
    public List<QualityIssue> check(BatteryData data) {
        // 通用检查逻辑
        return doCheck(data);
    }
    
    // 子类实现具体检查逻辑
    protected abstract List<QualityIssue> doCheck(BatteryData data);
}
```

### 观察者模式
用于实现事件通知机制，如质量问题监控：
```java
public class QualityMonitor {
    private List<QualityObserver> observers = new ArrayList<>();
    
    public void addObserver(QualityObserver observer) {
        observers.add(observer);
    }
    
    public void notifyQualityIssue(QualityIssue issue) {
        for (QualityObserver observer : observers) {
            observer.onQualityIssue(issue);
        }
    }
}
```

## 4. 核心类图

```mermaid
classDiagram
    class DataQualityApplication {
        +main(args: String[])
    }
    class ConfigManager {
        -instance: ConfigManager
        -config: AppConfig
        +getInstance(): ConfigManager
        +getConfig(): AppConfig
    }
    class BatteryData {
        -vin: String
        -timestamp: Long
        -soc: Integer
        -cellVoltages: List~Integer~
        +getVin(): String
    }
    interface IRule {
        +check(data: BatteryData): List~QualityIssue~
        +getType(): String
    }
    class AbstractRule {
        +check(data: BatteryData): List~QualityIssue~
        #createIssue(data: BatteryData, message: String): QualityIssue
        #singleIssue(data: BatteryData, message: String): List~QualityIssue~
        #noIssue(): List~QualityIssue~
    }
    class RuleEngine {
        -ruleCache: Map~String, IRule~
        +registerRule(rule: IRule, factories: List~String~): void
        +checkData(data: BatteryData, previousData: BatteryData, factoryId: String): List~QualityIssue~
    }
    class SocValidityRule {
        +check(data: BatteryData): List~QualityIssue~
    }
    class RuleProcessor {
        -previousDataState: ValueState~BatteryData~
        -ruleEngine: RuleEngine
        +processElement(data: BatteryData, ctx: Context, out: Collector): void
    }
    interface IStateRule {
        +checkState(current: BatteryData, previous: BatteryData): List~QualityIssue~
    }
    class AbstractStateRule {
        +checkState(current: BatteryData, previous: BatteryData): List~QualityIssue~
    }
    class RuleService {
        +loadRules(): void
        +registerRules(ruleEngine: RuleEngine): void
    }
    class ProcessedData {
        -batteryData: BatteryData
        -issues: List~QualityIssue~
        +toJson(): String
    }
    
    DataQualityApplication --> ConfigManager
    DataQualityApplication --> RuleProcessor
    RuleProcessor --> RuleEngine
    RuleProcessor --> RuleService
    RuleEngine --> IRule
    IRule <|-- AbstractRule
    AbstractRule <|-- SocValidityRule
    IRule <|-- IStateRule
    IStateRule <|-- AbstractStateRule
    RuleProcessor --> ProcessedData
```

## 5. 规则系统设计

### 规则分类
系统根据质量维度将规则分为四大类：
1. **有效性规则**：检查数据是否符合业务规则
2. **完整性规则**：检查数据是否完整
3. **一致性规则**：检查数据内部或与历史数据的一致性
4. **时效性规则**：检查数据时间戳是否满足要求

### 规则注解
使用注解简化规则的定义：
```java
@RuleDefinition(
    type = "SOC_VALIDITY",
    code = 1002,
    description = "SOC无效",
    category = RuleCategory.VALIDITY,
    priority = 5
)
public class SocValidityRule extends AbstractRule {
    // 规则实现
}
```

## 6. 配置说明

系统使用YAML格式配置文件：

```yaml
# Kafka配置
kafka:
  bootstrapServers: cdh03:6667,cdh04:6667,cdh05:6667
  topic: ods-001-wuling
  groupId: data-quality-group

# 处理配置  
process:
  parallelism: 1
  checkpointInterval: 60000

# Sink配置
sink:
  type: doris  # 可选：doris, print

# Doris配置
doris:
  conn: 10.2.96.62:8030
  user: root
  passwd: gotion@2025
  database: battery_ods
  table: error_data
```

## 7. 使用说明

### 编译打包
```bash
mvn clean package
```

### 运行系统
```bash
# 本地运行
java -jar target/data-quality-process.jar

# Flink集群运行
flink run -c org.battery.DataQualityApplication target/data-quality-process.jar
```

### 自定义参数
```bash
java -jar target/data-quality-process.jar \
  --kafka.bootstrapServers=localhost:9092 \
  --sink.type=print
```

## 8. 规则开发指南

### 创建新规则
1. 确定规则所属分类
2. 继承适当的抽象类（`AbstractRule`或`AbstractStateRule`）
3. 添加`@RuleDefinition`注解
4. 实现`check`或`checkState`方法

示例：
```java
@RuleDefinition(
    type = "CUSTOM_RULE",
    code = 2001,
    description = "自定义规则",
    category = RuleCategory.VALIDITY
)
public class CustomRule extends AbstractRule {
    @Override
    public List<QualityIssue> check(BatteryData data) {
        // 实现检查逻辑
        if (/* 条件 */) {
            return singleIssue(data, "发现问题");
        }
        return noIssue();
    }
}
```

## 9. 监控与告警

系统支持多种方式监控数据质量：
1. **日志监控**：详细记录质量问题
2. **指标监控**：输出关键指标到监控系统
3. **告警通知**：当质量问题超过阈值时触发告警

## 10. 常见问题与解答

### Q: 如何添加新的数据源？
A: 实现`SourceManager`接口并在主应用中注册。

### Q: 如何添加自定义Sink？
A: 实现`Sink`接口并在`SinkFactory`中注册。

### Q: 规则如何更新生效？
A: 系统会定期从数据库刷新规则，也可通过API触发手动更新。

## 11. 版本历史与规划

### 当前版本
- 实现基本的质量检测功能
- 支持动态规则加载
- 多种输出方式

### 规划特性
- 基于AI的异常检测
- 图形化规则管理界面
- 更丰富的指标与监控
- 性能优化
