package com.example.myapp.config;

import com.example.myapp.service.CustomUserDetailsService;
import com.example.myapp.service.LoginAttemptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The "enabled" check runs AFTER password verification (post-auth) instead of
     * Spring's default pre-auth check: a wrong password on a deactivated account
     * still reads as plain bad credentials, so anonymous visitors cannot probe
     * which accounts exist or are disabled. DisabledException therefore means
     * "correct password, deactivated account" and is safe to show openly.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(user -> {
            if (!user.isAccountNonLocked()) {
                throw new LockedException("حساب قفل شده است");
            }
            if (!user.isAccountNonExpired()) {
                throw new AccountExpiredException("حساب منقضی شده است");
            }
        });
        provider.setPostAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("حساب غیرفعال شده است");
            }
            if (!user.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException("رمز عبور منقضی شده است");
            }
        });
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginFailureHandler loginFailureHandler,
                                           LoginAttemptService loginAttempts) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/register/recovery-code", "/login",
                                "/forgot-password", "/error",
                                "/css/**", "/js/**", "/img/**", "/vendor/**", "/fonts/**", "/media/**",
                                "/robots.txt", "/sitemap.xml").permitAll()
                        // Public browsing and listening; the audio endpoint itself blocks
                        // non-approved tales for anyone but the owner or an admin.
                        .requestMatchers(HttpMethod.GET, "/tales", "/tales/*", "/tales/*/audio",
                                "/storytellers/*").permitAll()
                        .requestMatchers("/storyteller/**").hasRole("STORYTELLER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard")
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                // banned IPs/usernames are turned away before authentication runs
                .addFilterBefore(new LoginRateLimitFilter(loginAttempts), UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?loggedout"));
        return http.build();
    }
}
