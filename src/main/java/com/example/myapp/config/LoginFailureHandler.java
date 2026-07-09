package com.example.myapp.config;

import com.example.myapp.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Routes login failures to the right message and feeds the brute-force counter.
 * DisabledException only fires when the password was correct (see the post-auth
 * check in SecurityConfig), so it is shown openly and not counted as an attack.
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttempts;

    public LoginFailureHandler(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof DisabledException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?disabled");
            return;
        }
        String ip = request.getRemoteAddr();
        String username = request.getParameter("username");
        loginAttempts.recordFailure(ip, username);
        if (loginAttempts.isBanned(ip, username)) {
            getRedirectStrategy().sendRedirect(request, response, "/login?banned");
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, "/login?error");
    }
}
