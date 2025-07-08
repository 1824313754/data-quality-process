package org.battery.quality.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * 配置管理器
 * 单例模式，负责加载和管理应用配置
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    
    // 单例实例
    private static ConfigManager instance;
    
    // 应用配置
    private AppConfig config;
    
    /**
     * 私有构造函数
     */
    private ConfigManager() {
        loadConfig();
    }
    
    /**
     * 获取配置管理器实例
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * 获取应用配置
     */
    public AppConfig getConfig() {
        return config;
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream is = getClass().getClassLoader().getResourceAsStream("application.yml");
            
            if (is != null) {
                config = mapper.readValue(is, AppConfig.class);
                LOGGER.info("成功加载应用配置");
            } else {
                LOGGER.warn("未找到配置文件application.yml，使用默认配置");
                config = new AppConfig();
            }
        } catch (Exception e) {
            LOGGER.error("加载配置文件失败", e);
            config = new AppConfig();
        }
    }
} 