package com.systemdesign.razorpay.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

    /**
     * Plain (not @LoadBalanced) client for the auth call. A load-balanced builder bean here
     * broke the gateway's own Eureka client at startup. Auth goes to merchant-service by URL.
     */
    @Bean
    public RestClient authRestClient(GatewayRoutesProperties routes) {
        return RestClient.builder().baseUrl(routes.getAuthBaseUrl()).build();
    }
}
