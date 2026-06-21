package com.smartcloset.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

class TraceIdResponseFilterConfigurationTest {

    private static final int SPRING_SECURITY_FILTER_ORDER = -100;

    @Test
    void registersTraceIdFilterAfterObservationAndBeforeSecurity() {
        TraceIdResponseFilterConfiguration configuration = new TraceIdResponseFilterConfiguration();
        @SuppressWarnings("unchecked")
        ObjectProvider<io.micrometer.tracing.Tracer> tracerProvider = mock(ObjectProvider.class);
        TraceIdResponseFilter filter = configuration.traceIdResponseFilter(tracerProvider);

        FilterRegistrationBean<TraceIdResponseFilter> registration =
                configuration.traceIdResponseFilterRegistration(filter);

        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
        assertThat(registration.getOrder()).isLessThan(SPRING_SECURITY_FILTER_ORDER);
    }
}
