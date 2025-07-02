-- Doris 数据统计表
CREATE TABLE `normal_data_stats` (
  -- 维度字段
  `vin` varchar(255) NOT NULL COMMENT "车辆VIN码",
  `date` date NOT NULL COMMENT "数据日期", 
  `hour` smallint NULL COMMENT "小时(0-23)",
  `vehicleFactory` varchar(255) NULL COMMENT "车厂",
  
  -- 统计字段(使用聚合函数)
  `normal_data_count` bigint SUM NULL COMMENT "正常数据条数",
  `abnormal_data_count` bigint SUM NULL COMMENT "异常数据条数", 
  `data_count` bigint SUM NULL COMMENT "总数据条数",
  `time` datetime REPLACE NULL COMMENT "数据时间",
  `last_update_time` datetime REPLACE NULL COMMENT "最近更新时间"
) ENGINE=OLAP
AGGREGATE KEY(`vin`, `date`, `hour`, `vehicleFactory`)
PARTITION BY RANGE(`date`)()
DISTRIBUTED BY HASH(`vin`) BUCKETS AUTO
PROPERTIES (
  "replication_allocation" = "tag.location.offline: 1",
  "dynamic_partition.enable" = "true",
  "dynamic_partition.time_unit" = "DAY",
  "dynamic_partition.time_zone" = "Asia/Shanghai",
  "dynamic_partition.start" = "-30",
  "dynamic_partition.end" = "2",
  "dynamic_partition.prefix" = "p",
  "dynamic_partition.buckets" = "10",
  "storage_medium" = "hdd"
); 