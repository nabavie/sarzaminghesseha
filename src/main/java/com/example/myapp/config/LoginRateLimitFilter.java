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
        if ("POST".equalsIgnoreCase(request.getMethod()) && isLoginAttempt(request)
                && loginAttempts.isBanned(request.getRemoteAddr(), identity(request))) {
            response.sendRedirect(request.getContextPath() + "/login?banned");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isLoginAttempt(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/login".equals(path)
                || "/login/sms/send".equals(path)
                || "/login/sms/verify".equals(path);
    }

    private static String identity(HttpServletRequest request) {
        String username = request.getParameter("username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return request.getParameter("mobile");
    }
}
