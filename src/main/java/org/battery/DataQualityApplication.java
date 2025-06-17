package org.battery;

import org.battery.quality.job.DataQualityJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DataQualityApplication {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = SpringApplication.run(DataQualityApplication.class, args);
        DataQualityJob job = context.getBean(DataQualityJob.class);
        job.execute();
    }
} 