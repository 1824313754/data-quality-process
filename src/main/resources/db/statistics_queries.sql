-- 数据质量问题统计分析SQL

-- 1. 按车厂统计异常数量
SELECT 
    vehicleFactory AS 车厂,
    COUNT(*) AS 异常总数,
    COUNT(DISTINCT vin) AS 异常车辆数,
    AVG(issues_count) AS 平均异常数
FROM error_data
WHERE time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY vehicleFactory
ORDER BY COUNT(*) DESC;

-- 2. 按时间统计异常趋势
SELECT 
    DATE_FORMAT(time, '%Y-%m-%d') AS 日期,
    COUNT(*) AS 异常总数,
    COUNT(DISTINCT vin) AS 异常车辆数
FROM error_data
WHERE time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE_FORMAT(time, '%Y-%m-%d')
ORDER BY 日期;

-- 3. 提取异常类型统计
-- 注意：issues是array<text>类型，需要使用UNNEST函数展开
SELECT 
    issue_text AS 异常类型,
    COUNT(*) AS 异常数量,
    COUNT(DISTINCT vin) AS 影响车辆数
FROM (
    SELECT 
        vin,
        UNNEST(issues) AS issue_text
    FROM error_data
    WHERE time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
) t
GROUP BY issue_text
ORDER BY 异常数量 DESC;

-- 4. 特定车辆异常历史
SELECT 
    time AS 时间,
    issues AS 异常信息,
    issues_count AS 异常数量,
    vehicleStatus AS 车辆状态,
    chargeStatus AS 充电状态,
    speed AS 车速,
    totalVoltage AS 总电压,
    totalCurrent AS 总电流,
    soc AS SOC,
    maxTemperature AS 最高温度,
    minTemperature AS 最低温度
FROM error_data
WHERE vin = '具体车辆VIN'
ORDER BY time DESC;

-- 5. 高频异常车辆排名
SELECT 
    vin AS 车辆VIN,
    vehicleFactory AS 车厂,
    COUNT(*) AS 异常次数,
    AVG(issues_count) AS 平均异常数
FROM error_data
WHERE time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY vin, vehicleFactory
ORDER BY 异常次数 DESC
LIMIT 100;

-- 6. 按异常类型和车厂交叉统计
SELECT 
    issue_text AS 异常类型,
    vehicleFactory AS 车厂,
    COUNT(*) AS 异常数量
FROM (
    SELECT 
        vehicleFactory,
        UNNEST(issues) AS issue_text
    FROM error_data
    WHERE time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
) t
GROUP BY issue_text, vehicleFactory
ORDER BY 异常数量 DESC;

-- 7. 统计每日各车厂异常率(需要与正常数据表关联)
-- 假设有一个正常数据统计表daily_data_stats包含每日数据量信息
-- SELECT 
--     d.日期,
--     d.车厂,
--     e.异常数,
--     d.数据总量,
--     ROUND(e.异常数 * 100.0 / d.数据总量, 2) AS 异常率
-- FROM (
--     SELECT 
--         DATE_FORMAT(time, '%Y-%m-%d') AS 日期,
--         vehicleFactory AS 车厂,
--         COUNT(*) AS 异常数
--     FROM error_data
--     GROUP BY 日期, 车厂
-- ) e
-- JOIN daily_data_stats d ON e.日期 = d.日期 AND e.车厂 = d.车厂
-- ORDER BY d.日期 DESC, 异常率 DESC;

-- 8. 统计各种异常的严重程度分布
-- 假设我们基于issues字段内容对异常进行分类
SELECT 
    CASE 
        WHEN issue_text LIKE '%严重%' OR issue_text LIKE '%critical%' THEN '严重'
        WHEN issue_text LIKE '%警告%' OR issue_text LIKE '%warning%' THEN '警告'
        ELSE '一般'
    END AS 严重程度,
    COUNT(*) AS 异常数量
FROM (
    SELECT UNNEST(issues) AS issue_text
    FROM error_data
    WHERE time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
) t
GROUP BY 严重程度
ORDER BY 
    CASE 严重程度
        WHEN '严重' THEN 1
        WHEN '警告' THEN 2
        ELSE 3
    END;

-- 9. SOC异常分析
SELECT 
    vin,
    time,
    soc,
    totalVoltage,
    totalCurrent,
    chargeStatus,
    issues
FROM error_data
WHERE 
    issues_count > 0
    AND issues IS NOT NULL 
    AND ARRAY_CONTAINS(issues, '%SOC%')
ORDER BY time DESC
LIMIT 1000;

-- 10. 温度异常分析
SELECT 
    vin,
    time,
    maxTemperature,
    minTemperature,
    issues
FROM error_data
WHERE 
    issues_count > 0
    AND issues IS NOT NULL
    AND (ARRAY_CONTAINS(issues, '%温度%') OR ARRAY_CONTAINS(issues, '%Temperature%'))
ORDER BY time DESC
LIMIT 1000; 