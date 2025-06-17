package org.battery.quality.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * 应用配置加载器
 */
public class AppConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfigLoader.class);
    
    /**
     * 从YAML文件加载配置
     * @return 应用配置
     */
    public static AppConfig load() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream is = AppConfigLoader.class.getClassLoader().getResourceAsStream("application.yml");
            
            if (is != null) {
                AppConfig config = mapper.readValue(is, AppConfig.class);
                LOGGER.info("成功加载应用配置");
                return config;
            } else {
                LOGGER.warn("未找到配置文件application.yml，使用默认配置");
                return new AppConfig();
            }
        } catch (Exception e) {
            LOGGER.error("加载配置文件失败", e);
            return new AppConfig();
        }
    }
} 