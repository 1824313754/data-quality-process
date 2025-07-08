package org.battery.quality.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.battery.quality.model.BatteryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 电池数据反序列化模式
 * 用于将Kafka消息转换为BatteryData对象
 */
public class BatteryDataDeserializationSchema implements DeserializationSchema<BatteryData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatteryDataDeserializationSchema.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public BatteryData deserialize(byte[] message) throws IOException {
        try {
            // 解析JSON字符串
            JsonNode jsonNode = OBJECT_MAPPER.readTree(message);
            
            // 创建BatteryData对象
            BatteryData batteryData = new BatteryData();
            
            // 设置基本属性
            batteryData.setVin(jsonNode.path("vin").asText());
            batteryData.setTime(jsonNode.path("collectTime").asText());
            
            // 解析SOC
            if (!jsonNode.path("soc").isMissingNode()) {
                batteryData.setSoc(jsonNode.path("soc").asInt());
            }
            
            // 解析电压数组
            if (jsonNode.has("cellVoltages") && jsonNode.path("cellVoltages").isArray()) {
                List<Integer> voltages = new ArrayList<>();
                JsonNode voltagesNode = jsonNode.path("cellVoltages");
                for (JsonNode voltage : voltagesNode) {
                    voltages.add(voltage.asInt());
                }
                batteryData.setCellVoltages(voltages);
            }
            
            // 解析温度数组
            if (jsonNode.has("probeTemperatures") && jsonNode.path("probeTemperatures").isArray()) {
                List<Integer> temperatures = new ArrayList<>();
                JsonNode temperaturesNode = jsonNode.path("probeTemperatures");
                for (JsonNode temperature : temperaturesNode) {
                    temperatures.add(temperature.asInt());
                }
                batteryData.setProbeTemperatures(temperatures);
            }
            
            // 设置车辆状态信息
            if (!jsonNode.path("vehicleStatus").isMissingNode()) {
                batteryData.setVehicleStatus(jsonNode.path("vehicleStatus").asInt());
            }
            
            if (!jsonNode.path("chargeStatus").isMissingNode()) {
                batteryData.setChargeStatus(jsonNode.path("chargeStatus").asInt());
            }
            
            if (!jsonNode.path("speed").isMissingNode()) {
                batteryData.setSpeed(jsonNode.path("speed").asInt());
            }
            
            if (!jsonNode.path("mileage").isMissingNode()) {
                batteryData.setMileage(jsonNode.path("mileage").asInt());
            }
            
            // 设置电池信息
            if (!jsonNode.path("totalVoltage").isMissingNode()) {
                batteryData.setTotalVoltage(jsonNode.path("totalVoltage").asInt());
            }
            
            if (!jsonNode.path("totalCurrent").isMissingNode()) {
                batteryData.setTotalCurrent(jsonNode.path("totalCurrent").asInt());
            }
            
            if (!jsonNode.path("dcStatus").isMissingNode()) {
                batteryData.setDcStatus(jsonNode.path("dcStatus").asInt());
            }
            
            if (!jsonNode.path("insulationResistance").isMissingNode()) {
                batteryData.setInsulationResistance(jsonNode.path("insulationResistance").asInt());
            }
            
            // 位置信息
            if (!jsonNode.path("longitude").isMissingNode()) {
                batteryData.setLongitude(jsonNode.path("longitude").asLong());
            }
            
            if (!jsonNode.path("latitude").isMissingNode()) {
                batteryData.setLatitude(jsonNode.path("latitude").asLong());
            }
            
            return batteryData;
            
        } catch (Exception e) {
            LOGGER.error("解析电池数据失败: {}", new String(message), e);
            return null;
        }
    }

    @Override
    public boolean isEndOfStream(BatteryData nextElement) {
        return false;
    }

    @Override
    public TypeInformation<BatteryData> getProducedType() {
        return TypeInformation.of(BatteryData.class);
    }
} 