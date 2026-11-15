package de.paladinsinn.torg.codex.api.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsEndpointAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies production metrics export settings without a database or OTLP collector.
 */
class MetricsExportConfigurationTest {

    @Test
    void exportsMetricsToPrometheusButNotOtlp() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(
                        MetricsAutoConfiguration.class,
                        MetricsEndpointAutoConfiguration.class,
                        PrometheusMetricsExportAutoConfiguration.class,
                        OtlpMetricsExportAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(MetricsEndpoint.class)
                            .hasSingleBean(PrometheusScrapeEndpoint.class);
                    MeterRegistry registry = context.getBean(MeterRegistry.class);
                    assertThat(registry.getClass().getSimpleName()).isEqualTo("PrometheusMeterRegistry");
                    registry.counter("torg.codex.export.probe").increment();
                    assertThat(context.getBean(MetricsEndpoint.class).metric("torg.codex.export.probe", null))
                            .isNotNull();
                    assertThat(context.getEnvironment().getProperty(
                            "management.otlp.metrics.export.enabled", Boolean.class)).isFalse();
                    assertThat(context.getEnvironment().getProperty(
                            "management.tracing.export.otlp.enabled", Boolean.class)).isFalse();
                    assertThat(context.getEnvironment().getProperty(
                            "management.logging.export.otlp.enabled", Boolean.class)).isFalse();
                });
    }
}
