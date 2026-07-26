package com.knowledge.base.document.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Character encoding filter
 *
 * <p>Ensures all HTTP requests and responses use UTF-8 encoding, preventing garbled text issues</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@WebFilter(filterName = "CharacterEncodingFilter", urlPatterns = "/*")
public class CharacterEncodingFilter implements Filter {

    private static final String UTF_8 = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set the request encoding to UTF-8
        if (httpRequest.getCharacterEncoding() == null) {
            httpRequest.setCharacterEncoding(UTF_8);
        }

        // Set the response encoding to UTF-8
        httpResponse.setCharacterEncoding(UTF_8);
        httpResponse.setContentType("application/json;charset=UTF-8");

        // Continue the filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Destruction
    }
}
