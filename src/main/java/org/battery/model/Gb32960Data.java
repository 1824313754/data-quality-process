package org.battery.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Gb32960Data implements Serializable {
        private Integer maxTemperature;
        private Integer batteryNumber;
        private Integer year;
        private Integer soc;
        private Integer insulationResistance;
        private Integer maxVoltageSystemNum;
        private Integer seconds;
        private Integer minVoltagebatteryNum;
        private Integer temperatureProbeCount;
        @NotNull(message = "vin不能为空")
        private String vin;
        private String vehicleFactory;
        private Integer driveMotorCount;
        private Integer day;
        private Integer subsystemVoltageCount;
        private Integer gears;
        private Long longitude;  // 经度用Long表示
        private Integer mileage;
        private Integer subsystemTemperatureCount;
        private Integer level;
        private Integer maxTemperatureNum;
        private Integer minutes;
        private Integer minTemperatureNum;
        private Integer batteryCount;
        private Integer month;
        private Integer deviceFailuresCount;
        private Integer subsystemTemperatureDataNum;
        private Integer totalVoltage;
        private Integer vehicleStatus;
        private Integer status;
        private List<Integer> deviceFailuresCodes;
        private List<Integer> driveMotorFailuresCodes;
        private Integer maxVoltagebatteryNum;
        private Long latitude;   // 纬度用Long表示
        private Integer torque;
        private String alarmInfo;
        private List<Integer> cellVoltages;
        private Integer chargeStatus;
        private Integer speed;
        private Integer controllerInputVoltage;
        private Integer operationMode;
        private Integer current;
        private Integer cellCount;
        private Integer totalCurrent;
        private Integer minTemperature;
        private Integer temperature;
        private Integer batteryMaxVoltage;
        private Integer dcStatus;
        private Integer hours;
        private Integer riveMotorDataNum;
        private String brakingSystem;
        private Integer batteryMinVoltage;
        private Integer minTemperatureSystemNum;
        private Integer driveMotorFailuresCount;
        private Integer maxTemperatureSystemNum;
        private Integer voltage;
        private Integer subsystemVoltageDataNum;
        private Integer minVoltageSystemNum;
        private Integer otherFailuresCount;
        private List<Integer> probeTemperatures; // 你没给完整数组，按Integer处理
        private String customField;
        private Long Time;
        public List<Integer> getProbeTemperatures() {
                if (probeTemperatures == null) return null;
                return probeTemperatures.stream()
                        .map(t -> t == null ? null : t - 40)
                        .collect(Collectors.toList());
        }
        public Integer getCurrent() {
                return current == null ? null : current - 10000;
        }

        public Integer getTotalCurrent() {
                return totalCurrent == null ? null : totalCurrent - 10000;
        }


}