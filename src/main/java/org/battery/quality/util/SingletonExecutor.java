package org.battery.quality.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单例执行器，确保特定操作在多线程环境中只执行一次
 */
public class SingletonExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingletonExecutor.class);
    
    /**
     * 存储操作结果的Map，使用ConcurrentHashMap保证线程安全
     */
    private static final Map<String, Object> RESULT_MAP = new ConcurrentHashMap<>();
    
    /**
     * 执行操作，确保同一个key对应的操作只执行一次
     * 
     * @param key 操作的唯一标识
     * @param operation 要执行的操作
     * @param <T> 操作结果的类型
     * @return 操作的结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T executeOnce(String key, Supplier<T> operation) {
        // 利用ConcurrentHashMap的原子性computeIfAbsent方法确保操作只执行一次
        return (T) RESULT_MAP.computeIfAbsent(key, k -> {
            LOGGER.info("首次执行操作[{}]", key);
            try {
                return operation.get();
            } catch (Exception e) {
                LOGGER.error("执行操作[{}]失败", key, e);
                // 如果操作失败，从Map中移除对应的key，以便下次可以重试
                RESULT_MAP.remove(key);
                throw e;
            }
        });
    }
    
    /**
     * 执行无返回值的操作，确保同一个key对应的操作只执行一次
     * 
     * @param key 操作的唯一标识
     * @param operation 要执行的操作
     */
    public static void executeOnce(String key, Runnable operation) {
        executeOnce(key, () -> {
            operation.run();
            return null;
        });
    }
    
    /**
     * 清除特定key的执行记录和结果
     * 
     * @param key 操作的唯一标识
     */
    public static void clear(String key) {
        RESULT_MAP.remove(key);
        LOGGER.debug("已清除操作[{}]的执行记录和结果", key);
    }
    
    /**
     * 清除所有执行记录和结果
     */
    public static void clearAll() {
        RESULT_MAP.clear();
        LOGGER.debug("已清除所有操作的执行记录和结果");
    }
    
    /**
     * 检查特定操作是否已执行
     * 
     * @param key 操作的唯一标识
     * @return 如果操作已执行完成，返回true；否则返回false
     */
    public static boolean isExecuted(String key) {
        return RESULT_MAP.containsKey(key);
    }
} 