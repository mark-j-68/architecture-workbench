package com.architectureworkbench.discovery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Discovers Spring components, bean methods and directly observable injection points. */
public class SpringComponentDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("spring.component");
    private static final List<String> COMPONENTS = List.of("Service", "Component", "Repository", "Configuration");
    private static final Set<String> INJECTION = Set.of("Autowired", "Inject", "Resource");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Spring Component Discovery Plugin", "0.2.3", "Framework Plugin",
                List.of("java", "spring", "spring-boot"),
                List.of(DiscoveryPluginCapability.DETECT_SPRING_COMPONENTS, DiscoveryPluginCapability.DETECT_DEPENDENCY_INJECTION),
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
        String stereotype = COMPONENTS.stream().filter(name -> SpringDiscoverySupport.annotation(source.annotations(), name).isPresent())
                .findFirst().orElse("");
        if (stereotype.isBlank()) return;
        SpringDiscoverySupport.AnnotationUse annotation = SpringDiscoverySupport.annotation(source.annotations(), stereotype).orElseThrow();
        DiscoveryEvidence component = SpringDiscoverySupport.evidence(ID, "spring-component", source, source.qualifiedName(),
                annotation.line(), DiscoveryConfidence.observedFact("Spring stereotype annotation is explicitly present."), true,
                SpringDiscoverySupport.details("annotation", stereotype, "frameworkMarker", "@" + stereotype,
                        "symbol", source.className(), "componentKind", stereotype.toLowerCase()));
        evidence.add(component);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-component-declared",
                "Class " + source.className() + " is annotated with @" + stereotype + ".", component));

        addImplementedInterfaces(source, evidence, observations);
        for (SpringDiscoverySupport.Method method : source.methods()) {
            SpringDiscoverySupport.annotation(method.annotations(), "Bean").ifPresent(bean -> {
                DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-bean-method", source,
                        method.symbol(source), bean.line(), DiscoveryConfidence.observedFact("@Bean is explicitly present."), true,
                        SpringDiscoverySupport.details("annotation", "Bean", "frameworkMarker", "@Bean", "methodName", method.name(),
                                "symbol", method.symbol(source), "beanType", method.returnType()));
                evidence.add(item);
                observations.add(SpringDiscoverySupport.observation(ID, "spring-bean-declared",
                        method.symbol(source) + " declares a Spring bean of type " + method.returnType() + ".", item));
            });
            if (method.name().equals(source.className())) {
                for (SpringDiscoverySupport.Parameter parameter : SpringDiscoverySupport.parameters(method.parameters()))
                    addDependency(source, method.line(), source.className(), parameter.type(), parameter.name(), "constructor",
                            "constructor-parameter", evidence, observations);
            } else if (method.name().startsWith("set") && method.annotations().stream().anyMatch(use -> INJECTION.contains(use.name()))) {
                for (SpringDiscoverySupport.Parameter parameter : SpringDiscoverySupport.parameters(method.parameters()))
                    addDependency(source, method.line(), method.symbol(source), parameter.type(), parameter.name(), "setter",
                            "@" + method.annotations().stream().filter(use -> INJECTION.contains(use.name())).findFirst().orElseThrow().name(), evidence, observations);
            }
        }
        for (SpringDiscoverySupport.Field field : source.fields()) {
            SpringDiscoverySupport.AnnotationUse injection = field.annotations().stream().filter(use -> INJECTION.contains(use.name())).findFirst().orElse(null);
            if (injection != null)
                addDependency(source, field.line(), source.className() + "." + field.name(), field.type(), field.name(), "field",
                        "@" + injection.name(), evidence, observations);
        }
    }

    private static void addImplementedInterfaces(SpringDiscoverySupport.Source source, List<DiscoveryEvidence> evidence,
                                                 List<DiscoveryObservation> observations) {
        int marker = source.header().indexOf("implements");
        if (marker < 0) return;
        String declarations = source.header().substring(marker + "implements".length()).replaceAll("\\bpermits\\b.*", "");
        Arrays.stream(declarations.split(",")).map(String::trim).filter(value -> !value.isBlank()).forEach(value -> {
            String implemented = SpringDiscoverySupport.simpleType(value);
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-component-interface", source,
                    source.className() + "->" + implemented, source.typeLine(),
                    DiscoveryConfidence.observedFact("Implemented interface is explicit in the component declaration."), true,
                    SpringDiscoverySupport.details("annotation", "", "frameworkMarker", "implements", "symbol", source.className(),
                            "interfaceType", implemented));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-component-implements-interface",
                    source.className() + " implements " + implemented + ".", item));
        });
    }

    private static void addDependency(SpringDiscoverySupport.Source source, int line, String symbol, String dependencyType,
                                      String dependencyName, String kind, String marker, List<DiscoveryEvidence> evidence,
                                      List<DiscoveryObservation> observations) {
        String type = SpringDiscoverySupport.simpleType(dependencyType);
        if (type.isBlank() || isSimple(type)) return;
        DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-component-dependency", source,
                symbol + "->" + type + ":" + kind, line,
                DiscoveryConfidence.high("Dependency follows from a directly observable Spring component injection point."), false,
                SpringDiscoverySupport.details("annotation", marker.startsWith("@") ? marker.substring(1) : "",
                        "frameworkMarker", marker, "symbol", symbol, "dependencyType", type,
                        "dependencyName", dependencyName, "injectionKind", kind));
        evidence.add(item);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-component-depends-on-component",
                source.className() + " has a " + kind + " dependency on " + type + ".", item));
    }

    private static boolean isSimple(String type) {
        return Set.of("String", "boolean", "Boolean", "byte", "Byte", "short", "Short", "int", "Integer", "long", "Long",
                "float", "Float", "double", "Double", "char", "Character").contains(type);
    }
}
