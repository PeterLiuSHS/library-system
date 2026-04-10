package com.kexun.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration  // let Spring scan it when the service starts
public class RestClientConfig {

    @Bean // ask Spring to build an object and put into the container
    // provided by frames, so use @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
