package com.systemdesign.razorpay.merchant.security;

import com.systemdesign.razorpay.common.enums.UserRole;
import com.systemdesign.razorpay.merchant.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email, UUID merchantId, UserRole role){
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60*60)))
                .claim("merchant_id", merchantId)
                .claim("role",role)
                .signWith(getSecretKey())
                .compact();
    }

    public Claims validateAccessToken(String accessToken){
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String extractMerchantId(Claims claims) {
        return claims.get("merchant_id",String.class);
    }
}
