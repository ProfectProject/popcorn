package com.popcorn.checkIns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.popcorn.checkIns", "com.popcorn.demo.common"})
public class CheckInsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckInsApplication.class, args);
    }

}
