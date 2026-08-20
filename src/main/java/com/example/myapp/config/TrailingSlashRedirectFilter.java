package com.example.myapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * GET /path/ → 301 /path so crawlers do not see a 404 or a duplicate URL.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String uri = request.getRequestURI();
            if (uri != null && uri.length() > 1 && uri.endsWith("/")) {
                String target = uri.substring(0, uri.length() - 1);
                String query = request.getQueryString();
                if (query != null && !query.isBlank()) {
                    target = target + "?" + query;
                }
                response.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
                response.setHeader(HttpHeaders.LOCATION, target);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
