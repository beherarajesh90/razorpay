package com.systemdesign.razorpay.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

    /**
     * Load-balanced client. Hostnames like "merchant-service" resolve through Eureka.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient authRestClient(RestClient.Builder loadBalancedRestClientBuilder,
                                     GatewayRoutesProperties routes) {
        return loadBalancedRestClientBuilder.baseUrl(routes.getAuthBaseUrl()).build();
    }
}
