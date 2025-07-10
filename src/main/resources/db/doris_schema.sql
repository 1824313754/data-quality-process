-- Doris数据库表结构定义
-- 所有表都在battery_quality库下

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `battery_quality`;
USE `battery_quality`;

-- 规则配置表 (使用UNIQUE KEY支持主键覆盖更新，以rule_code作为主键)
CREATE TABLE IF NOT EXISTS `battery_quality`.`rule_class` (
  `rule_code` int NOT NULL COMMENT "异常编码",
  `id` varchar(100) NOT NULL COMMENT "规则ID",
  `name` varchar(100) NOT NULL COMMENT "规则名称",
  `description` varchar(255) NOT NULL COMMENT "规则描述",
  `category` varchar(50) NOT NULL COMMENT "规则分类",
  `priority` int NOT NULL COMMENT "规则优先级",
  `source_code` text NOT NULL COMMENT "规则源代码",
  `enabled_factories` varchar(1000) NOT NULL COMMENT "启用的车厂ID列表，用逗号分隔，0表示所有车厂",
  `create_time` datetime NOT NULL COMMENT "创建时间",
  `update_time` datetime NOT NULL COMMENT "更新时间",
  `status` tinyint NOT NULL COMMENT "规则状态：1-启用，0-禁用"
) ENGINE=OLAP
UNIQUE KEY(`rule_code`)
DISTRIBUTED BY HASH(`rule_code`) BUCKETS 10
PROPERTIES (
"replication_allocation" = "tag.location.offline: 1",
"storage_medium" = "hdd",
"storage_format" = "V2",
"enable_unique_key_merge_on_write" = "true"
);

-- 车厂ID参考：
-- 0: 默认所有车厂
-- 1: 五菱
-- 2: 江淮
-- 4: 瑞驰
-- 5: 吉利
-- 6: 奇瑞
-- 7: 奇瑞商用车
-- 13: 移动充电车
-- 14: 吉智
-- 15: 合众
-- 16: 广通
-- 17: 江淮商用车
-- 18: 吉利商用车
-- 19: 上汽大通
-- 20: 安凯
-- 21: 南京开沃
-- 22: 绿色慧联
-- 23: 电动屋
-- 24: 小康
-- 25: 三一
-- 26: 奇瑞商用车
-- 27: 南京建康
-- 28: 太和宇通
-- 29: 长江重卡
-- 30: 吉利重卡
-- 31: 凯翼

-- 电池数据统计表
CREATE TABLE IF NOT EXISTS `battery_quality`.`normal_data_stats` (
  `vin` varchar(255) NOT NULL COMMENT "车辆VIN码",
  `dayOfYear` date NOT NULL COMMENT "数据日期",
  `hour` smallint NULL COMMENT "小时(0-23)",
  `vehicleFactory` varchar(255) NULL COMMENT "车厂",
  `normalDataCount` bigint SUM NULL COMMENT "正常数据条数",
  `abnormalDataCount` bigint SUM NULL COMMENT "异常数据条数",
  `dataCount` bigint SUM NULL COMMENT "总数据条数",
  `time` datetime REPLACE NULL COMMENT "数据时间",
  `lastUpdateTime` datetime REPLACE NULL COMMENT "最近更新时间"
) ENGINE=OLAP
AGGREGATE KEY(`vin`, `dayOfYear`, `hour`, `vehicleFactory`)
PARTITION BY RANGE(`dayOfYear`)
()
DISTRIBUTED BY HASH(`vin`) BUCKETS AUTO
PROPERTIES (
"replication_allocation" = "tag.location.offline: 1",
"min_load_replica_num" = "-1",
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
"dynamic_partition.hot_partition_num" = "0",
"dynamic_partition.reserved_history_periods" = "NULL",
"dynamic_partition.storage_policy" = "",
"storage_medium" = "hdd",
"storage_format" = "V2",
"inverted_index_storage_format" = "V1",
"light_schema_change" = "true",
"disable_auto_compaction" = "false",
"enable_single_replica_compaction" = "false",
"group_commit_interval_ms" = "10000",
"group_commit_data_bytes" = "134217728"
);

-- 电池数据及质量问题表
CREATE TABLE IF NOT EXISTS `battery_quality`.`ods_data_with_issues` (
  `vin` varchar(255) NOT NULL COMMENT "车辆VIN码",
  `vehicleFactory` varchar(255) NULL COMMENT "车辆厂商代码",
  `time` datetime NOT NULL COMMENT "数据时间",
  `vehicleStatus` smallint NULL COMMENT "车辆状态",
  `chargeStatus` smallint NULL COMMENT "充电状态",
  `speed` int NULL COMMENT "车速",
  `mileage` int NULL COMMENT "里程",
  `totalVoltage` int NULL COMMENT "总电压",
  `totalCurrent` int NULL COMMENT "总电流",
  `soc` smallint NULL COMMENT "电池SOC",
  `dcStatus` smallint NULL COMMENT "DC-DC状态",
  `gears` smallint NULL COMMENT "档位",
  `insulationResistance` int NULL COMMENT "绝缘电阻",
  `operationMode` smallint NULL COMMENT "运行模式",
  `batteryCount` smallint NULL COMMENT "电池包数量",
  `batteryNumber` smallint NULL COMMENT "电池编号",
  `cellCount` smallint NULL COMMENT "电池单体数量",
  `maxVoltagebatteryNum` smallint NULL COMMENT "最高电压电池序号",
  `maxVoltageSystemNum` smallint NULL COMMENT "最高电压系统号",
  `batteryMaxVoltage` int NULL COMMENT "电池最高电压",
  `minVoltagebatteryNum` smallint NULL COMMENT "最低电压电池序号",
  `minVoltageSystemNum` smallint NULL COMMENT "最低电压系统号",
  `batteryMinVoltage` int NULL COMMENT "电池最低电压",
  `maxTemperature` smallint NULL COMMENT "最高温度",
  `maxTemperatureNum` smallint NULL COMMENT "最高温度探针序号",
  `maxTemperatureSystemNum` smallint NULL COMMENT "最高温度系统号",
  `minTemperature` smallint NULL COMMENT "最低温度",
  `minTemperatureNum` smallint NULL COMMENT "最低温度探针序号",
  `minTemperatureSystemNum` smallint NULL COMMENT "最低温度系统号",
  `subsystemVoltageCount` smallint NULL COMMENT "子系统电压数量",
  `subsystemVoltageDataNum` smallint NULL COMMENT "子系统电压数据编号",
  `subsystemTemperatureCount` smallint NULL COMMENT "子系统温度数量",
  `subsystemTemperatureDataNum` smallint NULL COMMENT "子系统温度数据编号",
  `temperatureProbeCount` smallint NULL COMMENT "温度探针数量",
  `longitude` bigint NULL COMMENT "经度",
  `latitude` bigint NULL COMMENT "纬度",
  `cellVoltages` string NULL COMMENT "电池单体电压列表",
  `probeTemperatures` string NULL COMMENT "温度探针列表",
  `deviceFailuresCodes` string NULL COMMENT "设备故障码列表",
  `driveMotorFailuresCodes` string NULL COMMENT "驱动电机故障码列表",
  `customField` string NULL COMMENT "自定义字段",
  `ctime` datetime NULL COMMENT "处理时间",
  `issues` string NULL COMMENT "质量问题"
) ENGINE=OLAP
DUPLICATE KEY(`vin`, `time`)
PARTITION BY RANGE(`time`)
()
DISTRIBUTED BY HASH(`vin`) BUCKETS AUTO
PROPERTIES (
"replication_allocation" = "tag.location.offline: 1",
"dynamic_partition.enable" = "true",
"dynamic_partition.time_unit" = "DAY",
"dynamic_partition.time_zone" = "Asia/Shanghai",
"dynamic_partition.start" = "-90",
"dynamic_partition.end" = "2",
"dynamic_partition.prefix" = "p",
"dynamic_partition.buckets" = "10"
); 