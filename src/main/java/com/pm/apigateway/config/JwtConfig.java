
package com.pm.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${security.jwt.public-key-path}") String publicKeyPath,
            @Value("${security.jwt.issuer}") String issuer)
            throws Exception {

        Path path = Path.of(publicKeyPath);
        String pem = Files.readString(path, StandardCharsets.UTF_8);

        String content = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(content);

        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(keyBytes);

        RSAPublicKey publicKey = (RSAPublicKey)
                KeyFactory.getInstance("RSA")
                        .generatePublic(keySpec);

        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder
                        .withPublicKey(publicKey)
                        .signatureAlgorithm(SignatureAlgorithm.RS256)
                        .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }
}