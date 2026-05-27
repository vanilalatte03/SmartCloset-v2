package com.smartcloset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartClosetApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartClosetApplication.class, args);
    }
}
