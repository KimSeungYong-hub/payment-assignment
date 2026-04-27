package com.practice.paymentassignment.filter;

import jakarta.servlet.ServletException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

//    public static final String REQUEST_URI = "requestUri";
//    public static final String HTTP_METHOD = "httpMethod";

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    jakarta.servlet.FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();

        try {
            MDC.put("requestId", requestId);
//            MDC.put(REQUEST_URI, requestUri);
//            MDC.put(HTTP_METHOD, httpMethod);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }


}
