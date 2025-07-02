-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS battery_quality DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE battery_quality;

-- 创建规则表
CREATE TABLE IF NOT EXISTS rule_class (
    id VARCHAR(100) NOT NULL COMMENT '规则ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    description VARCHAR(255) NOT NULL COMMENT '规则描述',
    category VARCHAR(50) NOT NULL COMMENT '规则分类',
    rule_code INT NOT NULL COMMENT '异常编码',
    priority INT NOT NULL DEFAULT 5 COMMENT '规则优先级',
    source_code MEDIUMTEXT NOT NULL COMMENT '规则源代码',
    enabled_factories VARCHAR(1000) NOT NULL DEFAULT '0' COMMENT '启用的车厂ID列表，用逗号分隔，0表示所有车厂',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_category (category),
    INDEX idx_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则表';

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