package karm.van.config;

import jakarta.annotation.PostConstruct;
import karm.van.config.properties.AdsMicroServiceProperties;
import karm.van.config.properties.AuthMicroServiceProperties;
import karm.van.config.properties.CommentMicroServiceProperties;
import karm.van.config.properties.ImageMicroServiceProperties;
import karm.van.exception.handling.CustomAuthenticationEntryPoint;
import karm.van.filter.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.concurrent.Executor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableJpaRepositories(basePackages = "karm.van.repo.jpaRepo")
@EnableElasticsearchRepositories(basePackages = "karm.van.repo.elasticRepo")
@RequiredArgsConstructor
@EnableAsync
@EnableConfigurationProperties({AdsMicroServiceProperties.class, ImageMicroServiceProperties.class, AuthMicroServiceProperties.class, CommentMicroServiceProperties.class})
public class SecurityConfiguration {

    private final JwtRequestFilter jwtRequestFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${microservices.auth.endpoints.recovery-password}")
    private String passwordRecoveryEndPoint;

    private String fullEndpoint;

    @PostConstruct
    public void init(){
        fullEndpoint = passwordRecoveryEndPoint+"/**";
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(5);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .authorizeHttpRequests(auth->auth.requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh-token",
                                "/user/recovery/**",
                                fullEndpoint,
                                "/user/addCard/**",
                                "/user/comment/del/**",
                                "/user/card/favorite/unlink/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(CsrfConfigurer::disable)
                .cors(cors->cors.configurationSource(corsConfigurationSource))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e->e.authenticationEntryPoint(customAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
