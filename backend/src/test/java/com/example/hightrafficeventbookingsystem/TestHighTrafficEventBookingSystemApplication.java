package com.example.hightrafficeventbookingsystem;

import org.springframework.boot.SpringApplication;

public class TestHighTrafficEventBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.from(HighTrafficEventBookingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
