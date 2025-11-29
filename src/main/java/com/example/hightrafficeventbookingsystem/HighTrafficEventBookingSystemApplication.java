package com.example.hightrafficeventbookingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HighTrafficEventBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighTrafficEventBookingSystemApplication.class, args);
    }

}
