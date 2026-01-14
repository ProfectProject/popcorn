package com.popcorn.demo.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Swagger UI 그룹별 개별 URL 제공 컨트롤러
 *
 * 사용법:
 * - 고객용: /swagger-ui/customer.html
 * - 운영자용: /swagger-ui/manager.html
 * - 전체: /swagger-ui/admin.html
 */
@Controller
public class SwaggerRedirectController {

    /**
     * 고객용 API 문서 (🛒)
     * URL: /swagger-ui/customer.html
     */
    @GetMapping("/swagger-ui/customer.html")
    public String customerSwagger() {
        return "redirect:/swagger-ui.html?urls.primaryName=" +
               java.net.URLEncoder.encode("🛒 고객용 API", java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 운영자용 API 문서 (🏪)
     * URL: /swagger-ui/manager.html
     */
    @GetMapping("/swagger-ui/manager.html")
    public String managerSwagger() {
        return "redirect:/swagger-ui.html?urls.primaryName=" +
               java.net.URLEncoder.encode("🏪 운영자용 API", java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 전체 API 문서 (🔧)
     * URL: /swagger-ui/admin.html
     */
    @GetMapping("/swagger-ui/admin.html")
    public String adminSwagger() {
        return "redirect:/swagger-ui.html?urls.primaryName=" +
               java.net.URLEncoder.encode("🔧 전체 API (개발자용)", java.nio.charset.StandardCharsets.UTF_8);
    }
}