package com.example.myapp.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tells crawlers not to index private, auth, or error responses (X-Robots-Tag).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NoIndexHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (shouldNoIndex(request, response)) {
            response.setHeader("X-Robots-Tag", "noindex, nofollow");
        }
    }

    private static boolean shouldNoIndex(HttpServletRequest request, HttpServletResponse response) {
        if (request.getDispatcherType() == DispatcherType.ERROR
                || request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null) {
            return true;
        }
        int status = response.getStatus();
        if (status >= 400) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        return path.startsWith("/admin")
                || path.startsWith("/dashboard")
                || path.startsWith("/storyteller/")
                || path.equals("/login")
                || path.startsWith("/login/")
                || path.equals("/register")
                || path.startsWith("/register/")
                || path.equals("/forgot-password")
                || path.equals("/feedback")
                || path.equals("/logout")
                || path.equals("/error");
    }
}
