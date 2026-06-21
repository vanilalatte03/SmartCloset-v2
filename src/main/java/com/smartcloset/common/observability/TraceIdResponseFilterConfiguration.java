package com.smartcloset.common.observability;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class TraceIdResponseFilterConfiguration {

    static final int TRACE_ID_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    @Bean
    public TraceIdResponseFilter traceIdResponseFilter(ObjectProvider<Tracer> tracerProvider) {
        return new TraceIdResponseFilter(tracerProvider);
    }

    @Bean
    public FilterRegistrationBean<TraceIdResponseFilter> traceIdResponseFilterRegistration(
            TraceIdResponseFilter traceIdResponseFilter
    ) {
        FilterRegistrationBean<TraceIdResponseFilter> registration = new FilterRegistrationBean<>(traceIdResponseFilter);
        registration.setOrder(TRACE_ID_FILTER_ORDER);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        return registration;
    }
}
