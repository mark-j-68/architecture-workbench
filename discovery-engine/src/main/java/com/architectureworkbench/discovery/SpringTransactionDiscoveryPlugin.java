package com.architectureworkbench.discovery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Discovers explicit Spring transaction boundaries without judging their placement. */
public class SpringTransactionDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("spring.transaction");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Spring Transaction Discovery Plugin", "0.2.3", "Framework Plugin",
                List.of("java", "spring", "spring-tx"),
                List.of(DiscoveryPluginCapability.DETECT_TRANSACTION_BOUNDARIES), SpringApplicationDiscoveryPlugin.dependencies(), true);
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics)) {
            SpringDiscoverySupport.annotation(source.annotations(), "Transactional").ifPresent(annotation ->
                    addBoundary(source, source.className(), "class", annotation, source.className(), evidence, observations));
            for (SpringDiscoverySupport.Method method : source.methods())
                SpringDiscoverySupport.annotation(method.annotations(), "Transactional").ifPresent(annotation ->
                        addBoundary(source, method.symbol(source), "method", annotation, method.name(), evidence, observations));
        }
        return DiscoveryPluginResult.succeeded(ID, new DiscoveryOutput(evidence, observations, diagnostics), Duration.between(started, Instant.now()));
    }

    private static void addBoundary(SpringDiscoverySupport.Source source, String symbol, String level,
                                    SpringDiscoverySupport.AnnotationUse annotation, String methodName,
                                    List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        String readOnly = normalized(SpringDiscoverySupport.namedValue(annotation.arguments(), "readOnly"));
        String propagation = enumValue(SpringDiscoverySupport.namedValue(annotation.arguments(), "propagation"));
        String isolation = enumValue(SpringDiscoverySupport.namedValue(annotation.arguments(), "isolation"));
        DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-transaction-boundary", source,
                source.qualifiedName() + ":" + symbol, annotation.line(),
                DiscoveryConfidence.observedFact("@Transactional is explicitly present."), true,
                SpringDiscoverySupport.details("annotation", "Transactional", "frameworkMarker", "@Transactional",
                        "methodName", level.equals("method") ? methodName : "", "symbol", symbol, "boundaryLevel", level,
                        "componentKind", componentKind(source), "readOnly", readOnly, "propagation", propagation,
                        "isolation", isolation));
        evidence.add(item);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-transaction-boundary-declared",
                symbol + " is transactional" + (readOnly.equals("true") ? " and read-only" : "") + ".", item));
    }

    private static String componentKind(SpringDiscoverySupport.Source source) {
        for (String name : List.of("RestController", "Controller", "Service", "Repository", "Component", "Configuration"))
            if (SpringDiscoverySupport.annotation(source.annotations(), name).isPresent()) return name.toLowerCase();
        return "other";
    }

    private static String enumValue(String value) {
        if (value.isBlank()) return "";
        int dot = value.lastIndexOf('.');
        return normalized(dot >= 0 ? value.substring(dot + 1) : value);
    }

    private static String normalized(String value) {
        return value.replaceAll("[}\\s]", "").trim();
    }
}
