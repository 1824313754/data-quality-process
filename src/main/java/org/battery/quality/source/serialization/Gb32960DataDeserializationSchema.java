package org.battery.quality.source.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.battery.model.Gb32960Data;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Gb32960DataDeserializationSchema implements DeserializationSchema<Gb32960Data> {
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Gb32960Data deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, Gb32960Data.class);
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