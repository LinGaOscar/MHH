package com.mhh.ap.config;

import com.mhh.ap.service.UserLogService;
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
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${mhh.auth.dev-mode:false}")
    private boolean devMode;

    private final PasswordEncoder passwordEncoder;
    private final UserLogService userLogService;

    public SecurityConfig(PasswordEncoder passwordEncoder, UserLogService userLogService) {
        this.passwordEncoder = passwordEncoder;
        this.userLogService = userLogService;
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
                    .defaultSuccessUrl("/dashboard", true)
                    .permitAll()
                )
                .logout(logout -> logout
                    .permitAll()
                    .logoutSuccessHandler((request, response, authentication) -> {
                        if (authentication != null) {
                            userLogService.record(authentication.getName(), "LOGOUT", "登出", "SUCCESS");
                        }
                        response.sendRedirect("/login?logout");
                    })
                );
        } else {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/css/**", "/js/**", "/lib/**", "/images/**", "/favicon.ico", "/sso/login").permitAll()
                    .anyRequest().authenticated()
                )
                .logout(logout -> logout
                    .permitAll()
                    .logoutSuccessHandler((request, response, authentication) -> {
                        if (authentication != null) {
                            userLogService.record(authentication.getName(), "LOGOUT", "登出", "SUCCESS");
                        }
                        new HttpStatusReturningLogoutSuccessHandler().onLogoutSuccess(request, response, authentication);
                    })
                )
                .httpBasic(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("1234"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
