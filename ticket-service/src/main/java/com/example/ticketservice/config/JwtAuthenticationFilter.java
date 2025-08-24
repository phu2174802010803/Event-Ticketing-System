package com.example.ticketservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String SECRET_KEY = "2a32cf15ccee5415ad13ac598110a98eec185bbf200dd625f8b6cb0b213ee9572d691c0a327a4b03563cdf19decef602b3117190da3c2ab527197231af5d42653a46eb6f1f6a604e1c830c5131602a8e9b5350921ae2e9e5a252b3065cf5daa6feedb2c6d0f6780d04257ca39942f61c496628c51119d47c204300f3ddaa2aae0c18b743142cafa4ff85ea293306b1c393d7324621de40d39229f081323d1ee3e6dd13f8f0208b33b4b206d7bff593feb6b83519004a3aaed0d86c390ef427ce7ae7fce34b4baa0738986aec8214259e12d2ef528717e2c14bee1d9bdf0cdc33cf83f558291ff61f04fea6cda58113f777a0467543ef06811f0215d148d98295";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Bỏ qua xác thực cho endpoint /api/tickets/confirm
        if ("/api/tickets/confirm".equals(path) ||
                (path.startsWith("/ws-native") && "Upgrade".equalsIgnoreCase(request.getHeader("Connection")))) {
            chain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(SECRET_KEY)
                        .parseClaimsJws(token)
                        .getBody();
                String username = claims.getSubject();
                Integer userId = claims.get("user_id", Integer.class);
                List<String> roles = claims.get("roles", List.class);
                String role = (roles != null && !roles.isEmpty()) ? roles.get(0) : null;

                if (userId != null && role != null) {
                    setSecurityContext(String.valueOf(userId), role);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }

    private void setSecurityContext(String userId, String role) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userId, null, Collections.singletonList(authority));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}