package com.architectureworkbench.discovery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Discovers Spring MVC controllers, advice, endpoints and direct web-layer references. */
public class SpringWebDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("spring.web");
    private static final Set<String> MAPPINGS = Set.of("RequestMapping", "GetMapping", "PostMapping", "PutMapping", "PatchMapping", "DeleteMapping");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Spring Web Discovery Plugin", "0.2.3", "Framework Plugin",
                List.of("java", "spring", "spring-web", "spring-boot"),
                List.of(DiscoveryPluginCapability.DETECT_HTTP_ENDPOINTS, DiscoveryPluginCapability.DETECT_DEPENDENCY_INJECTION),
                SpringApplicationDiscoveryPlugin.dependencies(), true);
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
        String controllerAnnotation = SpringDiscoverySupport.annotation(source.annotations(), "RestController").isPresent()
                ? "RestController" : SpringDiscoverySupport.annotation(source.annotations(), "Controller").isPresent() ? "Controller" : "";
        for (String advice : List.of("ControllerAdvice", "RestControllerAdvice")) {
            SpringDiscoverySupport.annotation(source.annotations(), advice).ifPresent(annotation -> addMarker(source, advice,
                    "spring-exception-advice", source.className(), annotation.line(), evidence, observations));
        }
        if (controllerAnnotation.isBlank()) return;
        SpringDiscoverySupport.AnnotationUse marker = SpringDiscoverySupport.annotation(source.annotations(), controllerAnnotation).orElseThrow();
        addMarker(source, controllerAnnotation, "spring-web-controller", source.className(), marker.line(), evidence, observations);

        String classPath = SpringDiscoverySupport.annotation(source.annotations(), "RequestMapping")
                .map(SpringWebDiscoveryPlugin::mappingPath).orElse("");
        for (SpringDiscoverySupport.Method method : source.methods()) {
            for (SpringDiscoverySupport.AnnotationUse annotation : method.annotations()) {
                if (!MAPPINGS.contains(annotation.name())) continue;
                String methodPath = mappingPath(annotation);
                String path = SpringDiscoverySupport.joinPath(classPath, methodPath);
                String httpMethod = httpMethod(annotation);
                boolean dynamic = SpringDiscoverySupport.dynamic(path);
                DiscoveryEvidence endpoint = SpringDiscoverySupport.evidence(ID, "spring-http-endpoint", source,
                        httpMethod + " " + path + "->" + method.symbol(source), annotation.line(), dynamic
                                ? DiscoveryConfidence.inferred(0.7, "Endpoint path contains a dynamic expression.")
                                : DiscoveryConfidence.high("Endpoint path and method are composed from explicit mapping annotations."), false,
                        SpringDiscoverySupport.details("annotation", annotation.name(), "frameworkMarker", "@" + annotation.name(),
                                "methodName", method.name(), "symbol", method.symbol(source), "httpMethod", httpMethod,
                                "classPath", classPath, "methodPath", methodPath, "endpointPath", path,
                                "uncertainty", dynamic ? "dynamic-expression" : ""));
                evidence.add(endpoint);
                observations.add(SpringDiscoverySupport.observation(ID, "spring-http-endpoint-mapped",
                        httpMethod + " " + path + " is handled by " + method.symbol(source) + ".", endpoint));
                addDtoReferences(source, method, annotation, evidence, observations);
            }
        }
        addControllerDependencies(source, evidence, observations);
    }

    private static void addMarker(SpringDiscoverySupport.Source source, String annotationName, String type, String symbol,
                                  int line, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, type, source, source.qualifiedName(), line,
                DiscoveryConfidence.observedFact("@" + annotationName + " is explicitly present."), true,
                SpringDiscoverySupport.details("annotation", annotationName, "frameworkMarker", "@" + annotationName, "symbol", symbol));
        evidence.add(item);
        observations.add(SpringDiscoverySupport.observation(ID, type + "-detected",
                "Class " + source.className() + " is annotated with @" + annotationName + ".", item));
    }

    private static String mappingPath(SpringDiscoverySupport.AnnotationUse annotation) {
        String named = SpringDiscoverySupport.namedValue(annotation.arguments(), "path");
        if (named.isBlank()) named = SpringDiscoverySupport.namedValue(annotation.arguments(), "value");
        String value = SpringDiscoverySupport.firstString(named.isBlank() ? annotation.arguments() : named);
        return value;
    }

    private static String httpMethod(SpringDiscoverySupport.AnnotationUse annotation) {
        if (!annotation.name().equals("RequestMapping"))
            return annotation.name().replace("Mapping", "").toUpperCase(Locale.ROOT);
        String method = SpringDiscoverySupport.namedValue(annotation.arguments(), "method");
        int marker = method.indexOf("RequestMethod.");
        if (marker >= 0) {
            String value = method.substring(marker + "RequestMethod.".length()).replaceAll("[^A-Za-z].*", "");
            if (!value.isBlank()) return value.toUpperCase(Locale.ROOT);
        }
        return "ANY";
    }

    private static void addControllerDependencies(SpringDiscoverySupport.Source source, List<DiscoveryEvidence> evidence,
                                                  List<DiscoveryObservation> observations) {
        for (SpringDiscoverySupport.Method method : source.methods()) {
            if (!method.name().equals(source.className())) continue;
            for (SpringDiscoverySupport.Parameter parameter : SpringDiscoverySupport.parameters(method.parameters())) {
                if (!SpringDiscoverySupport.simpleType(parameter.type()).endsWith("Service")) continue;
                String dependency = SpringDiscoverySupport.simpleType(parameter.type());
                DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-controller-service-dependency", source,
                        source.className() + "->" + dependency, method.line(),
                        DiscoveryConfidence.high("Controller dependency follows from a constructor parameter type."), false,
                        SpringDiscoverySupport.details("annotation", "", "frameworkMarker", "constructor-parameter",
                                "methodName", source.className(), "symbol", source.className(), "dependencyType", dependency,
                                "injectionKind", "constructor"));
                evidence.add(item);
                observations.add(SpringDiscoverySupport.observation(ID, "spring-controller-depends-on-service",
                        source.className() + " has a constructor dependency on " + dependency + ".", item));
            }
        }
    }

    private static void addDtoReferences(SpringDiscoverySupport.Source source, SpringDiscoverySupport.Method method,
                                         SpringDiscoverySupport.AnnotationUse mapping, List<DiscoveryEvidence> evidence,
                                         List<DiscoveryObservation> observations) {
        for (SpringDiscoverySupport.Parameter parameter : SpringDiscoverySupport.parameters(method.parameters())) {
            String type = SpringDiscoverySupport.simpleType(parameter.type());
            if (isDtoCandidate(type)) addDto(source, method, mapping, type, "request", evidence, observations);
        }
        String response = SpringDiscoverySupport.simpleType(method.returnType());
        List<String> generics = SpringDiscoverySupport.genericTypes(method.returnType());
        if (!generics.isEmpty()) response = generics.get(0);
        if (isDtoCandidate(response)) addDto(source, method, mapping, response, "response", evidence, observations);
    }

    private static void addDto(SpringDiscoverySupport.Source source, SpringDiscoverySupport.Method method,
                               SpringDiscoverySupport.AnnotationUse mapping, String type, String direction,
                               List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-web-dto-reference", source,
                method.symbol(source) + ":" + direction + ":" + type, method.line(),
                DiscoveryConfidence.high("DTO reference follows from an endpoint method signature."), false,
                SpringDiscoverySupport.details("annotation", mapping.name(), "frameworkMarker", "@" + mapping.name(),
                        "methodName", method.name(), "symbol", method.symbol(source), "dtoType", type, "direction", direction));
        evidence.add(item);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-endpoint-references-dto",
                method.symbol(source) + " references " + type + " as a " + direction + " type.", item));
    }

    private static boolean isDtoCandidate(String type) {
        return !type.isBlank() && !Set.of("void", "String", "boolean", "Boolean", "byte", "short", "int", "Integer",
                "long", "Long", "float", "double", "ResponseEntity", "HttpServletRequest", "HttpServletResponse",
                "Model", "ModelAndView", "Principal", "UUID", "Optional", "List", "Set", "Map").contains(type);
    }
}
