package com.community.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// ponytail: Gateway 是 Reactive，不能引入 community-common（webmvc），自建最小 JWT 解析
@Slf4j
@Component
public class JwtGatewayUtil {

    private final JWTVerifier verifier;

    public JwtGatewayUtil(@Value("${community.jwt.secret}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret)).build();
    }

    public Long getUidOrNull(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaim("uid").asLong();
        } catch (Exception e) {
            log.debug("JWT verify failed: {}", e.getMessage());
            return null;
        }
    }
}
