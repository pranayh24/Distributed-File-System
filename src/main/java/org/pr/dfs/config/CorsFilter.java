package org.pr.dfs.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set CORS headers for all requests
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, x-session_id, X-Session-ID");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();

        log.debug("CORS Filter - Method: {}, URI: {}", method, requestURI);

        // Handle preflight OPTIONS requests
        if ("OPTIONS".equals(method)) {
            log.info("Handling OPTIONS preflight request for: {}", requestURI);
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Continue with the filter chain for non-OPTIONS requests
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CORS Filter initialized");
    }

    @Override
    public void destroy() {
        log.info("CORS Filter destroyed");
    }
}
