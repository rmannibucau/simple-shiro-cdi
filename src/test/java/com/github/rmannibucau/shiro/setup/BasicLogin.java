package com.github.rmannibucau.shiro.setup;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Dependent
@WebFilter("/*")
public class BasicLogin implements Filter {
    @Inject
    private Subject subject;

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final String[] up = new String(
                Base64.getDecoder().decode(HttpServletRequest.class.cast(servletRequest).getHeader("Authorization").substring("Basic ".length())), StandardCharsets.UTF_8)
                .split(":");
        subject.login(new UsernamePasswordToken(up[0], up[1]));
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            subject.logout();
        }
    }
}
