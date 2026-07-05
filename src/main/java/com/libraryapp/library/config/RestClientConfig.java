package com.libraryapp.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient for domain-service. Calls to domain-service stay inside the private network
 */
@Slf4j
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient domainServiceRestClient(DomainServiceProperties domainServiceProperties) {
        return RestClient.builder()
                .baseUrl(domainServiceProperties.getBaseUrl())
                .requestInterceptor((request, body, execution) -> {
                    log.warn("DEBUG outgoing {} {} headers={}", request.getMethod(), request.getURI(), request.getHeaders());
                    var response = execution.execute(request, body);
                    log.warn("DEBUG incoming status={} headers={}", response.getStatusCode(), response.getHeaders());
                    return response;
                })
                .build();
    }
}
