package com.loopin.api.common.config;

import com.loopin.api.common.security.CustomAccessDeniedHandler;
import com.loopin.api.common.security.CustomAuthEntryPoint;
import com.loopin.api.common.security.JwtAuthFilter;
import com.loopin.api.common.ratelimit.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.Customizer;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomAuthEntryPoint customAuthEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Qualifier("devLoginAccessCustomizer") ObjectProvider<Customizer<HttpSecurity>> devLoginAccess
    ) throws Exception {
        devLoginAccess.ifAvailable(customizer -> customizer.customize(http));

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/api/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers("/v1/auth/**", "/api/v1/auth/**").permitAll()
                .requestMatchers("/ws", "/ws/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/users/register", "/api/v1/users/register").permitAll()
                .requestMatchers("/v1/users/me", "/api/v1/users/me").authenticated()
                .requestMatchers("/v1/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/v1/users/**", "/api/v1/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/events/**", "/api/v1/events/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/groups/**", "/api/v1/groups/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    /**
     * Development login is deliberately available only in non-production profiles.
     * Security matchers exclude the servlet context path (/api).
     */
    @Bean("devLoginAccessCustomizer")
    @Profile({"local", "dev", "test"})
    public Customizer<HttpSecurity> devLoginAccess() {
        return http -> http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/v1/dev/auth/login").permitAll()
        );
    }



    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-User-Id", "X-User-Role"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
