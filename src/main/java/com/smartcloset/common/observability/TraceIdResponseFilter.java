package com.smartcloset.common.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 현재 request trace id를 응답 header로 노출해 운영 로그와 사용자 제보를 연결할 수 있게 한다.
 */
public class TraceIdResponseFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final ObjectProvider<Tracer> tracerProvider;

    public TraceIdResponseFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        addTraceIdHeader(response);
        filterChain.doFilter(request, response);
    }

    private void addTraceIdHeader(HttpServletResponse response) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return;
        }
        Span span = tracer.currentSpan();
        if (span == null || span.context() == null || span.context().traceId() == null) {
            return;
        }
        response.setHeader(TRACE_ID_HEADER, span.context().traceId());
    }
}
