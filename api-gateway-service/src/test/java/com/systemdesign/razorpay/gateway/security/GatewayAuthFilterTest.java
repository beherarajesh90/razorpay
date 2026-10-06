package com.systemdesign.razorpay.gateway.security;

import com.sun.net.httpserver.HttpServer;
import com.systemdesign.razorpay.gateway.config.GatewayRoutesProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Edge auth: public routes skip auth, protected routes need merchant-service to accept the
 * Authorization header, and identity headers are rewritten (client-sent values never pass through).
 * merchant-service is replaced by a local HTTP stub.
 */
class GatewayAuthFilterTest {

    private static final String GOOD_KEY = "Basic Z29vZC1rZXk=";
    private static final UUID MERCHANT = UUID.fromString("8f08bfbc-e734-45bf-b3a0-80b1d7764170");

    private HttpServer merchantStub;
    private GatewayAuthFilter filter;

    @BeforeEach
    void startStub() throws IOException {
        merchantStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        merchantStub.createContext("/internal/v1/auth/verify", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (GOOD_KEY.equals(auth)) {
                byte[] body = ("{\"merchantId\":\"" + MERCHANT + "\",\"keyId\":\"rzp_test_it\",\"subject\":null,\"role\":null}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else {
                exchange.sendResponseHeaders(401, -1);
            }
            exchange.close();
        });
        merchantStub.start();

        GatewayRoutesProperties routes = new GatewayRoutesProperties();
        routes.setAuthBaseUrl("http://localhost:" + merchantStub.getAddress().getPort());
        routes.setPublicPaths(List.of("/v1/auth/login", "/webhook/**"));
        RestClient client = RestClient.builder().baseUrl(routes.getAuthBaseUrl()).build();
        filter = new GatewayAuthFilter(client, routes);
    }

    @AfterEach
    void stopStub() {
        merchantStub.stop(0);
    }

    @Test
    void publicRouteSkipsAuthAndStripsClientIdentityHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/auth/login");
        request.addHeader("X-Merchant-Id", UUID.randomUUID().toString());
        request.addHeader("X-Key-Id", "rzp_test_spoofed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        HttpServletRequest seen = captureForwarded(chain);
        assertThat(seen.getHeader("X-Merchant-Id")).isNull();
        assertThat(seen.getHeader("X-Key-Id")).isNull();
    }

    @Test
    void protectedRouteWithoutAuthorizationIsRejectedWithoutCallingMerchant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectedCredentialsReturnMerchantStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/orders");
        request.addHeader("Authorization", "Basic d3Jvbmc=");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validCredentialsForwardMerchantIdentityAndHideAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/orders");
        request.addHeader("Authorization", GOOD_KEY);
        request.addHeader("X-Merchant-Id", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        HttpServletRequest seen = captureForwarded(chain);
        assertThat(seen.getHeader("X-Merchant-Id")).isEqualTo(MERCHANT.toString());
        assertThat(seen.getHeader("X-Key-Id")).isEqualTo("rzp_test_it");
        assertThat(seen.getHeader("Authorization")).isNull();
    }

    private HttpServletRequest captureForwarded(FilterChain chain) throws Exception {
        var captor = org.mockito.ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        verify(chain).doFilter(captor.capture(), any());
        return (HttpServletRequest) captor.getValue();
    }
}
