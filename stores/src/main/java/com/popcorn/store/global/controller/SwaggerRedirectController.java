package com.popcorn.store.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerRedirectController {

    @GetMapping("/webjars/swagger-ui/index.html")
    public String redirectToSwagger() {
        return "forward:/swagger-ui/index.html";
    }
}
