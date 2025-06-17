package org.battery.quality.model;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisResult implements Serializable {
    private String vin;
    private boolean hasIssue;
    private List<String> issues;
    private long timestamp;
    private boolean isWindowAnalysis;

    public AnalysisResult() {
        this.issues = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public void addIssue(String issue) {
        this.issues.add(issue);
        this.hasIssue = true;
    }

    public void addIssues(List<String> newIssues) {
        this.issues.addAll(newIssues);
        if (!newIssues.isEmpty()) {
            this.hasIssue = true;
        }
    }
} 