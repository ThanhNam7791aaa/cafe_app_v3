package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {"com.example.demo"})  
public class CafeAppVer1Application {
    public static void main(String[] args) {
        SpringApplication.run(CafeAppVer1Application.class, args);
    }
}