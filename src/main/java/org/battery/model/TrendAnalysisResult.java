package org.battery.model;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class TrendAnalysisResult implements Serializable {
    private String vin;
    private boolean hasAnomaly;
    private List<String> anomalyMessages;
    private long startTime;
    private long endTime;

    public TrendAnalysisResult() {
        this.anomalyMessages = new ArrayList<>();
    }

    public void addAnomaly(String message) {
        this.anomalyMessages.add(message);
        this.hasAnomaly = true;
    }
} 