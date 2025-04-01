// src/main/java/com/example/identityservice/util/JwtUtil.java
package com.example.identityservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "2a32cf15ccee5415ad13ac598110a98eec185bbf200dd625f8b6cb0b213ee9572d691c0a327a4b03563cdf19decef602b3117190da3c2ab527197231af5d42653a46eb6f1f6a604e1c830c5131602a8e9b5350921ae2e9e5a252b3065cf5daa6feedb2c6d0f6780d04257ca39942f61c496628c51119d47c204300f3ddaa2aae0c18b743142cafa4ff85ea293306b1c393d7324621de40d39229f081323d1ee3e6dd13f8f0208b33b4b206d7bff593feb6b83519004a3aaed0d86c390ef427ce7ae7fce34b4baa0738986aec8214259e12d2ef528717e2c14bee1d9bdf0cdc33cf83f558291ff61f04fea6cda58113f777a0467543ef06811f0215d148d98295";
    private static final long JWT_TOKEN_VALIDITY = 10 * 60 * 60 * 1000; // 10 giờ

    public String generateToken(Integer userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId); // Thêm userId làm claim tùy chỉnh
        claims.put("roles", roles);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username) // Giữ username làm subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject); // Trả về username
    }

    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("user_id", Integer.class)); // Trả về userId
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}