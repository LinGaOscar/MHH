package com.mhh.ap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${mhh.auth.dev-mode:false}")
    private boolean devMode;

    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (devMode) {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/css/**", "/js/**", "/lib/**", "/images/**", "/favicon.ico", "/sso/login").permitAll()
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                    .defaultSuccessUrl("/dashboard", true) // 登入成功後改去 Dashboard，避免無限循環於 /
                    .permitAll()
                )
                .logout(logout -> logout.permitAll());
        } else {
            // 正式模式：僅允許 SSO 路徑與靜態資源
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/css/**", "/js/**", "/lib/**", "/images/**", "/favicon.ico", "/sso/login").permitAll()
                    .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // 提供基礎攔截，或是視需要關閉
        }
        
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // 固定 Dev 模式下的 admin/1234
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("1234"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
