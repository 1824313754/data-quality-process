package org.battery.quality.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.battery.quality.model.Gb32960Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 国标32960数据反序列化Schema
 */
public class Gb32960DeserializationSchema implements DeserializationSchema<Gb32960Data> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Gb32960DeserializationSchema.class);
    private transient ObjectMapper objectMapper;
    
    @Override
    public Gb32960Data deserialize(byte[] message) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        
        try {
            return objectMapper.readValue(message, Gb32960Data.class);
        } catch (Exception e) {
            LOGGER.error("反序列化数据失败: {}", new String(message), e);
            return null;
        }
    }

    @Override
    public boolean isEndOfStream(Gb32960Data nextElement) {
        return false;
    }

    @Override
    public TypeInformation<Gb32960Data> getProducedType() {
        return TypeInformation.of(Gb32960Data.class);
    }
} 