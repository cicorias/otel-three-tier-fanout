package wt.consumer.ehconsumer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import com.azure.spring.integration.core.EventHubHeaders;
import com.azure.spring.integration.core.api.reactor.Checkpointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
import static com.azure.spring.integration.core.AzureHeaders.CHECKPOINTER;

@SpringBootApplication
public class EhconsumerApplication {

    public static final Logger LOGGER = LoggerFactory.getLogger(EhconsumerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EhconsumerApplication.class, args);
    }

    public String getCorrelationId() {

        //broken in// implementation 'com.azure.spring:azure-spring-cloud-stream-binder-eventhubs:2.9.0'  // breaks getting correlationid from span
        Span current = null;
        SpanContext context = null;
        String traceid = null;

        current = Span.current();

        if (null != current)
            context = current.getSpanContext();

        if (null != current && null != context)
            traceid = context.getTraceId();

        return traceid;

    }

    public String getAICorrelationId() {
        var requestTelemetryContext = ThreadContext.getRequestTelemetryContext();
        RequestTelemetry requestTelemetry = requestTelemetryContext == null ? null
                : requestTelemetryContext.getHttpRequestTelemetry();
        String correlationId = requestTelemetry == null ? null : requestTelemetry.getContext().getOperation().getId();

        return correlationId;
    }

    @Bean
    public Consumer<Message<JsonNode>> consume() {
        return message -> {
            Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);

            var thing = getCorrelationId();
            // var thing2 = getAICorrelationId();

            LOGGER.warn("**********  correlationid={}", thing);

            LOGGER.warn(
                    "New message received: '{}', diagnostic-id: {},  partition key: {}, partition id: {}, raw partition id: {}, sequence number: {}, offset: {}, enqueued time: {}",
                    message.getPayload(),
                    message.getHeaders().get("diagnostic-id"),
                    message.getHeaders().get(EventHubHeaders.PARTITION_KEY),
                    message.getHeaders().get(EventHubHeaders.PARTITION_ID),
                    message.getHeaders().get(EventHubHeaders.RAW_PARTITION_ID),
                    message.getHeaders().get(EventHubHeaders.SEQUENCE_NUMBER),
                    message.getHeaders().get(EventHubHeaders.OFFSET),
                    message.getHeaders().get(EventHubHeaders.ENQUEUED_TIME));
            checkpointer.success()
                    .doOnSuccess(success -> LOGGER.info("Message '{}' successfully checkpointed", message.getPayload()))
                    .doOnError(error -> LOGGER.error("Exception found", error)).subscribe();
        };
    }
}
