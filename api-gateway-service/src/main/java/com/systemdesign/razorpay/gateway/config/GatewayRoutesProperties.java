package com.systemdesign.razorpay.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway-level settings. Public paths skip authentication; everything else needs a valid
 * API key (Basic) or JWT (Bearer), verified by merchant-service.
 */
@Component
@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class GatewayRoutesProperties {

    private String authBaseUrl = "http://merchant-service";

    private List<String> publicPaths = new ArrayList<>();
}
