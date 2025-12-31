package com.popcorn.demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.popcorn.demo.application",
        "com.popcorn.demo.domain",
        "com.popcorn.demo.infrastructure",
        "com.popcorn.demo.common",
        "com.popcorn.demo.presentation"
})
public class OrderTestApplication {
}
