package org.battery.quality.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 国标32960电池数据实体类
 */
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
    
    private String vin;
    
    private String vehicleFactory;
    private Integer driveMotorCount;
    private Integer day;
    private Integer subsystemVoltageCount;
    private Integer gears;
    
    private Long longitude;
    
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
    
    private Long latitude;
    
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
    private List<Integer> probeTemperatures;
    private String customField;
    private Long time;
    private Long ctime;


    public List<Integer> computeProbeTemperatures() {
        if (probeTemperatures == null) return null;
        return probeTemperatures.stream()
                .map(t -> t == null ? null : t - 40)
                .collect(Collectors.toList());
    }
    
    public Integer computeCurrent() {
        return current == null ? null : current - 10000;
    }

    public Integer computeTotalCurrent() {
        return totalCurrent == null ? null : totalCurrent - 10000;
    }
    
    public long computeCtime() {
        int fullYear = (year < 100) ? (2000 + year) : year;
        LocalDateTime dateTime = LocalDateTime.of(fullYear, month, day, hours, minutes, seconds);
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
    
    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
        updateCtimeIfReady();
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
        updateCtimeIfReady();
    }

    public void setHours(Integer hours) {
        this.hours = hours;
        updateCtimeIfReady();
    }

    public void setDay(Integer day) {
        this.day = day;
        updateCtimeIfReady();
    }

    public void setMonth(Integer month) {
        this.month = month;
        updateCtimeIfReady();
    }

    public void setYear(Integer year) {
        this.year = year;
        updateCtimeIfReady();
    }

    private void updateCtimeIfReady() {
        if (year != null && month != null && day != null && hours != null && minutes != null && seconds != null) {
            this.ctime = computeCtime();
        }
    }

    public void setTotalCurrent(Integer totalCurrent) {
        this.totalCurrent = totalCurrent;
        this.totalCurrent = computeTotalCurrent();
    }
    
    public void setCtime(Long ctime) {
        this.ctime = ctime;
        this.ctime = computeCtime();
    }
    
    public void setCurrent(Integer current) {
        this.current = current;
        this.current = computeCurrent();
    }

    public void setProbeTemperatures(List<Integer> probeTemperatures) {
        this.probeTemperatures = probeTemperatures;
        this.probeTemperatures = computeProbeTemperatures();
    }
} 