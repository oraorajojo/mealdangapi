package com.example.mealdangapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(FastApiProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient fastApiClient(FastApiProperties props) {
        // JDK HttpClient가 기본값으로 HTTP/2(h2c) 업그레이드를 시도하는데,
        // uvicorn이 이를 지원하지 않아 요청 바디가 유실된다. HTTP/1.1로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        return RestClient.builder()
            .baseUrl(props.baseUrl())
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();
    }
}
