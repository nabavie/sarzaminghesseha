package com.example.myapp.config;

import com.example.myapp.service.LoginAttemptService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects login POSTs from banned IPs/usernames before authentication runs,
 * so even a correct password cannot get through while the ban lasts.
 * Registered manually in SecurityConfig (not a @Component) to keep it out of
 * the servlet container's global filter chain.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttempts;

    public LoginRateLimitFilter(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath())
                && loginAttempts.isBanned(request.getRemoteAddr(), request.getParameter("username"))) {
            response.sendRedirect(request.getContextPath() + "/login?banned");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
