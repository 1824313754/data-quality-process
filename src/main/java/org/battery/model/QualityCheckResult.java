package org.battery.model;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class QualityCheckResult implements Serializable {
    private String vin;
    private boolean passed;
    private List<String> errorMessages;
    private long timestamp;

    public QualityCheckResult() {
        this.errorMessages = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public void addError(String error) {
        this.errorMessages.add(error);
        this.passed = false;
    }
} 