package com.careerpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan("com.careerpilot")
public class CareerpilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerpilotApplication.class, args);
    }
}
