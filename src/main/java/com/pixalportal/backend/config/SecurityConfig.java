package com.pixalportal.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // This grabs the secret key we defined in application.yml
    @Value("${supabase.jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Enable Cross-Origin Resource Sharing (CORS) so localhost:3000 can talk to it
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. Disable CSRF since we are building a stateless, token-based API
            .csrf(csrf -> csrf.disable()) 
            
            // 3. Define the authorization rules for your endpoints
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll() // Open endpoints
                .anyRequest().authenticated() // All other endpoints require a valid JWT
            )
            
            // 4. Configure Spring to act as an OAuth2 Resource Server using JWTs
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }

    /**
     * Creates the decoder that validates the HS256 JWTs using your Supabase secret.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Supabase uses HS256, which maps to HmacSHA256 in Java
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        
        // Build and return the decoder configured with your symmetric key
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Configures CORS to explicitly allow requests from your Next.js application.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow your Next.js frontend origin
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        
        // Allow the standard HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow the Authorization header (which carries the token) and Content-Type
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this CORS configuration to all paths
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}