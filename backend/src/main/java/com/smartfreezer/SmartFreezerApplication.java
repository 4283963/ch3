package com.smartfreezer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartFreezerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartFreezerApplication.class, args);
    }
}
