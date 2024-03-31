//package com.adp.site.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.Customizer;
//
//import java.time.Duration;
//
//@Configuration
//public class RateLimiterConfig {
//    @Bean
//    public Customizer<RateLimiterConfig> rateLimiterCustomizer() {
//        return rateLimiterConfig -> rateLimiterConfig
//                .limitForPeriod(10) // Maximum allowed requests within the defined duration
//                .limitRefreshPeriod(Duration.ofMinutes(1)); // Duration for the rate limit window
//    }
//}