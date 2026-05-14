package com.guitu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GuituApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuituApplication.class, args);
    }
}
