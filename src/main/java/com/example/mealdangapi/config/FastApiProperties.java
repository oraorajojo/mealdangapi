package com.example.mealdangapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fastapi")
public record FastApiProperties(String baseUrl) {
}
