package com.retaildashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * RestTemplate 및 RetryTemplate Bean 설정.
 *
 * - RestTemplate: Bearer 토큰 인터셉터 적용
 * - RetryTemplate: 최대 3회 재시도, 지수 백오프 (초기 1초, 배수 2.0, 최대 10초)
 *
 * Requirements: 1.2, 1.5
 */
@Configuration
public class RestClientConfig {

    @Value("${mock.api.token:mock-api-secret-token-2024}")
    private String apiToken;

    /**
     * Mock API 호출용 RestTemplate Bean.
     * Authorization: Bearer {token} 헤더를 자동으로 추가합니다.
     */
    @Bean
    public RestTemplate mockApiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor bearerTokenInterceptor = (request, body, execution) -> {
            request.getHeaders().setBearerAuth(apiToken);
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(List.of(bearerTokenInterceptor));
        return restTemplate;
    }

    /**
     * RetryTemplate Bean.
     * 최대 3회 재시도, 지수 백오프 정책을 적용합니다.
     *
     * - maxAttempts: 3
     * - initialInterval: 1000ms
     * - multiplier: 2.0
     * - maxInterval: 10000ms
     *
     * Requirements: 1.5
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
