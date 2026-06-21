package com.smartcloset.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

class ObservabilityPropertiesTest {

    @Test
    void applicationDefaultsEnableStructuredLogsAndTraceSampling() throws IOException {
        PropertySourcesPropertyResolver resolver = applicationPropertyResolver();
        resolver.setPlaceholderPrefix("${");
        resolver.setPlaceholderSuffix("}");
        resolver.setValueSeparator(":");

        assertThat(resolver.getProperty("logging.structured.format.console")).isEqualTo("ecs");
        assertThat(resolver.getProperty("logging.structured.ecs.service.name")).isEqualTo("smartcloset");
        assertThat(resolver.getProperty("logging.structured.ecs.service.environment")).isEqualTo("local");
        assertThat(resolver.getProperty("management.logging.export.otlp.enabled")).isEqualTo("false");
        assertThat(resolver.getProperty("management.otlp.metrics.export.enabled")).isEqualTo("false");
        assertThat(resolver.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0");
        assertThat(resolver.getProperty("management.tracing.export.otlp.enabled")).isEqualTo("false");
        assertThat(resolver.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
                .isEqualTo("http://localhost:4318/v1/traces");
    }

    private PropertySourcesPropertyResolver applicationPropertyResolver() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "application",
                new ClassPathResource("application.yml")
        );
        MutablePropertySources propertySources = new MutablePropertySources();
        sources.forEach(propertySources::addLast);
        propertySources.addLast(new PropertySource<>("springApplicationName") {
            @Override
            public Object getProperty(String name) {
                if ("spring.application.name".equals(name)) {
                    return "smartcloset";
                }
                return null;
            }
        });
        return new PropertySourcesPropertyResolver(propertySources);
    }
}
