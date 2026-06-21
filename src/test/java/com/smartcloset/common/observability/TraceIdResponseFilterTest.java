package com.smartcloset.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdResponseFilterTest {

    @Test
    void writesCurrentTraceIdToResponseHeader() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");

        MockHttpServletResponse response = new MockHttpServletResponse();

        new TraceIdResponseFilter(providerReturning(tracer))
                .doFilter(new MockHttpServletRequest("GET", "/api/weather/current"), response, noopChain());

        assertThat(response.getHeader(TraceIdResponseFilter.TRACE_ID_HEADER))
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void skipsHeaderWhenTracerIsUnavailable() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new TraceIdResponseFilter(providerReturning(null))
                .doFilter(new MockHttpServletRequest("GET", "/api/weather/current"), response, noopChain());

        assertThat(response.getHeader(TraceIdResponseFilter.TRACE_ID_HEADER)).isNull();
    }

    private ObjectProvider<Tracer> providerReturning(Tracer tracer) {
        @SuppressWarnings("unchecked")
        ObjectProvider<Tracer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(tracer);
        return provider;
    }

    private FilterChain noopChain() {
        return (request, response) -> {
        };
    }
}
