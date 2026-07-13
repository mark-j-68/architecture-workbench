package com.architectureworkbench.discovery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers Spring and cloud messaging listeners and direct publishing references. */
public class SpringMessagingDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("spring.messaging");
    private static final Set<String> LISTENERS = Set.of("EventListener", "TransactionalEventListener", "KafkaListener", "RabbitListener", "SqsListener");
    private static final List<String> PUBLISHERS = List.of("ApplicationEventPublisher", "KafkaTemplate", "RabbitTemplate", "SqsTemplate", "EventBridgeClient");
    private static final Pattern STATIC_DESTINATION = Pattern.compile("\\.(?:topic|queue|busName|eventBusName|detailType)\\s*\\(\\s*\"([^\"]+)\"");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Spring Messaging Discovery Plugin", "0.2.3", "Framework Plugin",
                List.of("java", "spring", "kafka", "rabbitmq", "sqs", "eventbridge"),
                List.of(DiscoveryPluginCapability.DETECT_MESSAGING_INTEGRATION), SpringApplicationDiscoveryPlugin.dependencies(), true);
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics))
            inspect(source, evidence, observations);
        return DiscoveryPluginResult.succeeded(ID, new DiscoveryOutput(evidence, observations, diagnostics), Duration.between(started, Instant.now()));
    }

    private static void inspect(SpringDiscoverySupport.Source source, List<DiscoveryEvidence> evidence,
                                List<DiscoveryObservation> observations) {
        for (SpringDiscoverySupport.Method method : source.methods()) {
            for (SpringDiscoverySupport.AnnotationUse annotation : method.annotations()) {
                if (!LISTENERS.contains(annotation.name())) continue;
                String destination = destination(annotation);
                List<SpringDiscoverySupport.Parameter> parameters = SpringDiscoverySupport.parameters(method.parameters());
                String eventType = parameters.isEmpty() ? "" : SpringDiscoverySupport.simpleType(parameters.get(0).type());
                boolean dynamic = SpringDiscoverySupport.dynamic(destination);
                DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-message-listener", source,
                        method.symbol(source) + ":" + annotation.name() + ":" + destination, annotation.line(), dynamic
                                ? DiscoveryConfidence.inferred(0.7, "Listener destination contains a dynamic expression.")
                                : DiscoveryConfidence.observedFact("Messaging listener annotation is explicitly present."), !dynamic,
                        SpringDiscoverySupport.details("annotation", annotation.name(), "frameworkMarker", "@" + annotation.name(),
                                "methodName", method.name(), "symbol", method.symbol(source), "listenerKind", annotation.name(),
                                "destination", destination, "eventType", eventType, "uncertainty", dynamic ? "dynamic-expression" : ""));
                evidence.add(item);
                String target = !eventType.isBlank() ? eventType : !destination.isBlank() ? destination : "messages";
                observations.add(SpringDiscoverySupport.observation(ID, "spring-message-consumer-declared",
                        method.symbol(source) + " consumes " + target + ".", item));
            }
        }
        for (String publisher : PUBLISHERS) {
            if (!source.content().contains(publisher)) continue;
            String operation = operation(source.content(), publisher);
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-message-publisher-reference", source,
                    source.qualifiedName() + ":" + publisher, source.typeLine(),
                    DiscoveryConfidence.observedFact("Messaging publishing abstraction is directly referenced."), true,
                    SpringDiscoverySupport.details("annotation", "", "frameworkMarker", publisher, "symbol", source.className(),
                            "publisherType", publisher, "publishOperation", operation));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-message-publisher-referenced",
                    source.className() + " references messaging publisher " + publisher + ".", item));
        }
        if (source.content().contains("putEvents(")) {
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "eventbridge-put-events-usage", source,
                    source.qualifiedName() + ":putEvents", source.typeLine(),
                    DiscoveryConfidence.observedFact("EventBridge putEvents usage is directly present."), true,
                    SpringDiscoverySupport.details("annotation", "", "frameworkMarker", "putEvents", "symbol", source.className(),
                            "publisherType", "EventBridge", "publishOperation", "putEvents"));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, "eventbridge-events-published",
                    source.className() + " invokes EventBridge putEvents.", item));
        }
        Matcher destination = STATIC_DESTINATION.matcher(source.content());
        while (destination.find()) {
            String value = destination.group(1);
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "messaging-static-destination", source,
                    source.qualifiedName() + ":" + value, line(source.content(), destination.start()),
                    DiscoveryConfidence.observedFact("Messaging destination or event detail type is statically declared."), true,
                    SpringDiscoverySupport.details("annotation", "", "frameworkMarker", destination.group(), "symbol", source.className(),
                            "destination", value));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, "messaging-destination-declared",
                    source.className() + " statically references messaging destination " + value + ".", item));
        }
    }

    private static String destination(SpringDiscoverySupport.AnnotationUse annotation) {
        for (String name : List.of("topics", "queues", "queueNames", "value")) {
            String named = SpringDiscoverySupport.namedValue(annotation.arguments(), name);
            if (!named.isBlank()) {
                String value = SpringDiscoverySupport.firstString(named);
                if (!value.isBlank()) return value;
            }
        }
        return SpringDiscoverySupport.firstString(annotation.arguments());
    }

    private static String operation(String content, String publisher) {
        if (publisher.equals("ApplicationEventPublisher") && content.contains("publishEvent(")) return "publishEvent";
        if (publisher.equals("RabbitTemplate") && content.contains("convertAndSend(")) return "convertAndSend";
        if (publisher.equals("EventBridgeClient") && content.contains("putEvents(")) return "putEvents";
        if (content.contains(".send(")) return "send";
        return "reference";
    }

    private static int line(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) if (content.charAt(i) == '\n') line++;
        return line;
    }
}
