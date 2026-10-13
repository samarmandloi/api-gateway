
package com.pm.apigateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class ApiKeyFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final byte[] expectedKey;

    public ApiKeyFilter(@Value("${security.api-key}") String apiKey) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }


    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        var request = exchange.getRequest();
        var method = request.getMethod();
        var path = request.getPath().value();

        // Allow viewing products without an API key.
        if (method == org.springframework.http.HttpMethod.GET
                && (path.equals("/api/v1/products")
                || path.matches("/api/v1/products/[^/]+"))) {
            return chain.filter(exchange);
        }

        String providedKey = request.getHeaders()
                .getFirst(API_KEY_HEADER);

        if (providedKey == null || !MessageDigest.isEqual(
                expectedKey,
                providedKey.getBytes(StandardCharsets.UTF_8))) {

            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            byte[] body = """
                {"error":"Unauthorized","message":"Missing or invalid API key"}
                """.getBytes(StandardCharsets.UTF_8);

            return response.writeWith(Mono.just(
                    response.bufferFactory().wrap(body)
            ));
        }

        return chain.filter(exchange);
    }

}
