package wt.consumer.ehconsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import reactor.core.publisher.Sinks;

@RestController
public class EventProducerController {

    public static final Logger LOGGER = LoggerFactory.getLogger(EventProducerController.class);

    /** this retrieves the OpenTelemetry representation of traceid */
    public String getCorrelationId() {

        Span current = null;
        SpanContext context = null;
        String traceid = null;

        current = Span.current();

        if (null != current) {
            context = current.getSpanContext();
        }

        if (null != current && null != context) {
            traceid = context.getTraceId();
        }

        return traceid;
    }

    /** this retrieves the ApplicationInsights legacy approach for correlationId */
    public String getAICorrelationId() {
        var requestTelemetryContext = ThreadContext.getRequestTelemetryContext();
        RequestTelemetry requestTelemetry = requestTelemetryContext == null ? null
                : requestTelemetryContext.getHttpRequestTelemetry();
        String correlationId = requestTelemetry == null ? null : requestTelemetry.getContext().getOperation().getId();

        return correlationId;
    }

    @Autowired
    private Sinks.Many<Message<String>> many;

    @Autowired
    private Sinks.Many<Message<String[]>> lots;

    @PostMapping("/messages")
    public ResponseEntity<String> sendMessage(@RequestBody JsonNode content) {
        try {
            String traceparent = content.at("/traceparent").asText();
            String message = content.at("/traceparent").asText();
            Response response = new Response();
            response.aiCorrelationId = getAICorrelationId();
            response.otCorrelationId = getCorrelationId();
            response.message = message;
            response.traceParent = traceparent;
            response.parentId = traceparent.split("-")[2];
    
            many.emitNext(MessageBuilder.withPayload(response.toString()).build(), Sinks.EmitFailureHandler.FAIL_FAST);

            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }


    }

    // @PostMapping("/many")
    // public ResponseEntity<String> sendMessages(@RequestParam String message) {
    // LOGGER.info("Going to add message {} to sendMessage.", message);

    // var response = new Response();
    // response.aiCorrelationId = getAICorrelationId();
    // response.otCorrelationId = getCorrelationId();
    // response.message = message;

    // many.emitNext(MessageBuilder.withPayload(message).build(),
    // Sinks.EmitFailureHandler.FAIL_FAST);
    // return ResponseEntity.ok(response.toString());
    // }

    class Response {
        public String aiCorrelationId;
        public String otCorrelationId;
        public String traceParent;
        public String parentId;
        public String message;

        @Override
        public String toString() {
            return String.format(
                    "{\"aiCorrelationId\": \"%s\" ,\n\"otCorrelationId\": \"%s\",\n\"message\": \"%s\",\n\"traceparent\": \"%s\",\n\"parentid\": \"%s\"}",
                    aiCorrelationId, otCorrelationId, message, traceParent, parentId);
        }
    }
}
