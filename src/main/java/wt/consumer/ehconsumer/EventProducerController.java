package wt.consumer.ehconsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    final ObjectMapper mapper = new ObjectMapper();

    /** this retrieves the OpenTelemetry representation of traceid */
    public String[] getCorrelationId() {

        Span current = null;
        SpanContext context = null;
        String traceid = null;
        String spanid = null;

        current = Span.current();

        if (null != current) {
            context = current.getSpanContext();
        }

        if (null != current && null != context) {
            traceid = context.getTraceId();
            spanid = context.getSpanId();
        }

        return new String[] { traceid, spanid };
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

    @PostMapping("/messages")
    public ResponseEntity<String> sendMessage(@RequestBody JsonNode content) {
        try {
            String traceparent = content.at("/traceparent").asText();
            String message = content.at("/message").asText();
            String[] otCorrelation = getCorrelationId();
            Response response = new Response();
            response.aiCorrelationId = getAICorrelationId();
            response.otCorrelationId = otCorrelation[0];
            response.otSpandId = otCorrelation[1];
            response.message = message;
            response.traceParent = traceparent;
            String[] traceParts = traceparent.split("-");
            if (traceParts.length > 1) {
                response.parentId = traceParts[2];
                response.clientProvided = true;
            } else {
                response.parentId = null;
            }

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
        
        public boolean clientProvided = false;
        public String aiCorrelationId;
        public String otCorrelationId;
        public String otSpandId;
        public String traceParent;
        public String parentId;
        public String message;

        @Override
        public String toString() {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                LOGGER.error("could not convert Response to json", e);
                return null;
            }
            // return String.format(
            // "{\"aiCorrelationId\": \"%s\" ,\n\t\"otCorrelationId\":
            // \"%s\",\n\t\"message\": \"%s\",\n\t\"traceparent\": \"%s\",\n\t\"parentid\":
            // \"%s\",\n\t\"clientprovided\": \"%s\"}",
            // aiCorrelationId, otCorrelationId, otSpandId, message, traceParent, parentId,
            // clientProvided);
        }
    }
}
