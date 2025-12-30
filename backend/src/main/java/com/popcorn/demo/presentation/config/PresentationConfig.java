package com.popcorn.demo.presentation.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.popcorn.demo.presentation")
public class PresentationConfig {
    // 컨트롤러/프레젠테이션 컴포넌트 스캔
}
