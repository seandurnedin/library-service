package com.libraryapp.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient for domain-service. Calls to domain-service stay inside the private network
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient domainServiceRestClient(DomainServiceProperties domainServiceProperties) {
        return RestClient.builder().baseUrl(domainServiceProperties.getBaseUrl()).build();
    }
}
