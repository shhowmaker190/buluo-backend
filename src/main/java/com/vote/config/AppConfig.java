package com.vote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String baseUrl = "http://localhost:5173";
    
    private boolean emailNotificationEnabled = true;
    
    private boolean localNotificationEnabled = true;
}
