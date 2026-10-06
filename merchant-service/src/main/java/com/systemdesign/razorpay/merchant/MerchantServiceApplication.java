package com.systemdesign.razorpay.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * scanBasePackages pulls in common-lib beans (config, rate limiter, audit, exception handler).
 */
@SpringBootApplication(scanBasePackages = "com.systemdesign.razorpay")
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@ConfigurationPropertiesScan
public class MerchantServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MerchantServiceApplication.class, args);
	}

}
