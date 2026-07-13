package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers Spring Boot entry points, configuration, component scans and profiles. */
public class SpringApplicationDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("spring.application");
    private static final Pattern PROFILE_PROPERTY = Pattern.compile("(?m)^\\s*(spring\\.profiles\\.(?:active|include|default)|spring\\.config\\.activate\\.on-profile)\\s*[:=]\\s*([^#\\r\\n]+)");
    private static final Pattern YAML_PROFILE = Pattern.compile("(?m)^\\s*(?:active|include|default|on-profile)\\s*:\\s*([^#\\r\\n]+)");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Spring Application Discovery Plugin", "0.2.3", "Framework Plugin",
                List.of("java", "spring", "spring-boot"),
                List.of(DiscoveryPluginCapability.DETECT_SPRING_APPLICATIONS, DiscoveryPluginCapability.DETECT_SPRING_CONFIGURATION),
                dependencies(), true);
    }

    static List<DiscoveryPluginDependency> dependencies() {
        return List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                new DiscoveryPluginDependency(MavenDiscoveryPlugin.ID, false),
                new DiscoveryPluginDependency(JavaStructureDiscoveryPlugin.ID, true));
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics)) {
            inspectSource(source, evidence, observations);
        }
        for (Path file : SpringDiscoverySupport.configurationFiles(input.rootDirectory(), diagnostics)) {
            inspectConfiguration(input.rootDirectory(), file, evidence, observations, diagnostics);
        }
        return DiscoveryPluginResult.succeeded(ID, new DiscoveryOutput(evidence, observations, diagnostics), Duration.between(started, Instant.now()));
    }

    private static void inspectSource(SpringDiscoverySupport.Source source, List<DiscoveryEvidence> evidence,
                                      List<DiscoveryObservation> observations) {
        addClassAnnotation(source, "SpringBootApplication", "spring-boot-application", "Spring Boot application", evidence, observations);
        addClassAnnotation(source, "Configuration", "spring-configuration-class", "Spring configuration class", evidence, observations);

        SpringDiscoverySupport.annotation(source.annotations(), "ComponentScan").ifPresent(annotation -> {
            DiscoveryEvidence marker = SpringDiscoverySupport.evidence(ID, "spring-component-scan", source,
                    source.qualifiedName(), annotation.line(), DiscoveryConfidence.observedFact("@ComponentScan is explicitly present."), true,
                    SpringDiscoverySupport.details("annotation", "ComponentScan", "symbol", source.className(), "arguments", annotation.arguments()));
            evidence.add(marker);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-component-scan-declared",
                    "Class " + source.className() + " declares @ComponentScan.", marker));
            List<String> packages = SpringDiscoverySupport.stringValues(annotation.arguments());
            for (String scanPackage : packages) {
                boolean dynamic = SpringDiscoverySupport.dynamic(scanPackage);
                DiscoveryEvidence scan = SpringDiscoverySupport.evidence(ID, "spring-component-scan-package", source,
                        source.qualifiedName() + "->" + scanPackage, annotation.line(), dynamic
                                ? DiscoveryConfidence.inferred(0.7, "Scan package contains a dynamic expression.")
                                : DiscoveryConfidence.observedFact("Scan base package is statically declared."), !dynamic,
                        SpringDiscoverySupport.details("annotation", "ComponentScan", "symbol", source.className(),
                                "scanBasePackage", scanPackage, "uncertainty", dynamic ? "dynamic-expression" : ""));
                evidence.add(scan);
                observations.add(SpringDiscoverySupport.observation(ID, "spring-component-scan-package-declared",
                        "Class " + source.className() + " scans Spring package " + scanPackage + ".", scan));
            }
        });
        SpringDiscoverySupport.annotation(source.annotations(), "SpringBootApplication").ifPresent(annotation -> {
            String declared = SpringDiscoverySupport.namedValue(annotation.arguments(), "scanBasePackages");
            for (String scanPackage : SpringDiscoverySupport.stringValues(declared))
                addScanPackage(source, annotation, scanPackage, "SpringBootApplication", evidence, observations);
        });

        SpringDiscoverySupport.annotation(source.annotations(), "Profile").ifPresent(annotation ->
                addProfiles(source, annotation, "annotation", evidence, observations));
        SpringDiscoverySupport.annotation(source.annotations(), "ActiveProfiles").ifPresent(annotation ->
                addProfiles(source, annotation, "test-annotation", evidence, observations));

        boolean hasMain = source.methods().stream().anyMatch(method -> method.name().equals("main"));
        if (hasMain && source.content().contains("SpringApplication.run")) {
            SpringDiscoverySupport.Method main = source.methods().stream().filter(method -> method.name().equals("main")).findFirst().orElseThrow();
            DiscoveryEvidence entry = SpringDiscoverySupport.evidence(ID, "spring-boot-entry-point", source,
                    main.symbol(source), main.line(), DiscoveryConfidence.observedFact("main invokes SpringApplication.run."), true,
                    SpringDiscoverySupport.details("annotation", "SpringApplication.run", "frameworkMarker", "SpringApplication.run",
                            "methodName", main.name(), "symbol", main.symbol(source)));
            evidence.add(entry);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-boot-entry-point-detected",
                    source.className() + ".main is a Spring Boot runtime entry point.", entry));
        }
    }

    private static void addClassAnnotation(SpringDiscoverySupport.Source source, String annotationName, String type,
                                           String label, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        SpringDiscoverySupport.annotation(source.annotations(), annotationName).ifPresent(annotation -> {
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, type, source, source.qualifiedName(), annotation.line(),
                    DiscoveryConfidence.observedFact("@" + annotationName + " is explicitly present."), true,
                    SpringDiscoverySupport.details("annotation", annotationName, "frameworkMarker", "@" + annotationName,
                            "symbol", source.className()));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, type + "-detected",
                    "Class " + source.className() + " is annotated with @" + annotationName + ".", item));
        });
    }

    private static void inspectConfiguration(Path root, Path file, List<DiscoveryEvidence> evidence,
                                             List<DiscoveryObservation> observations, List<String> diagnostics) {
        String name = file.getFileName().toString();
        String kind = name.startsWith("bootstrap") ? "bootstrap" : "application";
        DiscoveryEvidence config = SpringDiscoverySupport.fileEvidence(ID, "spring-configuration-file", root, file,
                SpringDiscoverySupport.relative(root, file), DiscoveryConfidence.observedFact("Spring configuration file exists."),
                SpringDiscoverySupport.details("configurationKind", kind, "frameworkMarker", name, "annotation", ""));
        evidence.add(config);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-configuration-file-detected",
                "Spring " + kind + " configuration file detected: " + SpringDiscoverySupport.relative(root, file) + ".", config));
        Matcher filenameProfile = Pattern.compile("(?:application|bootstrap)-([^.]+)\\.(?:properties|ya?ml)").matcher(name);
        if (filenameProfile.matches()) addFileProfile(root, file, filenameProfile.group(1), "filename", evidence, observations, false);
        try {
            String content = Files.readString(file);
            addConfigurationProperties(root, file, content, evidence, observations);
            Matcher property = PROFILE_PROPERTY.matcher(content);
            while (property.find()) {
                for (String value : property.group(2).replace("[", "").replace("]", "").split(","))
                    addFileProfile(root, file, value.trim().replace("\"", "").replace("'", ""), property.group(1), evidence, observations,
                            SpringDiscoverySupport.dynamic(value));
            }
            if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                Matcher yaml = YAML_PROFILE.matcher(content);
                while (yaml.find()) addFileProfile(root, file, yaml.group(1).trim().replace("\"", "").replace("'", ""),
                        "yaml-profile-key", evidence, observations, SpringDiscoverySupport.dynamic(yaml.group(1)));
            }
        } catch (IOException exception) {
            diagnostics.add("Unable to inspect " + SpringDiscoverySupport.relative(root, file) + ": " + exception.getMessage());
        }
    }

    private static void addScanPackage(SpringDiscoverySupport.Source source, SpringDiscoverySupport.AnnotationUse annotation,
                                       String scanPackage, String marker, List<DiscoveryEvidence> evidence,
                                       List<DiscoveryObservation> observations) {
        boolean dynamic = SpringDiscoverySupport.dynamic(scanPackage);
        DiscoveryEvidence scan = SpringDiscoverySupport.evidence(ID, "spring-component-scan-package", source,
                source.qualifiedName() + "->" + scanPackage, annotation.line(), dynamic
                        ? DiscoveryConfidence.inferred(0.7, "Scan package contains a dynamic expression.")
                        : DiscoveryConfidence.observedFact("Scan base package is statically declared."), !dynamic,
                SpringDiscoverySupport.details("annotation", marker, "frameworkMarker", "@" + marker,
                        "symbol", source.className(), "scanBasePackage", scanPackage,
                        "uncertainty", dynamic ? "dynamic-expression" : ""));
        evidence.add(scan);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-component-scan-package-declared",
                "Class " + source.className() + " scans Spring package " + scanPackage + ".", scan));
    }

    private static void addConfigurationProperties(Path root, Path file, String content,
                                                   List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        String name = file.getFileName().toString();
        if (name.endsWith(".properties")) {
            Matcher matcher = Pattern.compile("(?m)^\\s*([^#!\\s][^:=\\r\\n]*?)\\s*[:=]\\s*([^\\r\\n]*)").matcher(content);
            while (matcher.find()) addConfigurationProperty(root, file, matcher.group(1).trim(), matcher.group(2).trim(),
                    content.substring(0, matcher.start()).split("\\R", -1).length, evidence, observations);
            return;
        }
        List<String> parents = new ArrayList<>();
        int line = 0;
        for (String rawLine : content.split("\\R", -1)) {
            line++;
            if (rawLine.isBlank() || rawLine.stripLeading().startsWith("#") || !rawLine.contains(":")) continue;
            int indent = rawLine.length() - rawLine.stripLeading().length();
            int level = indent / 2;
            String stripped = rawLine.strip();
            String key = stripped.substring(0, stripped.indexOf(':')).trim();
            String value = stripped.substring(stripped.indexOf(':') + 1).trim();
            while (parents.size() > level) parents.remove(parents.size() - 1);
            if (value.isBlank()) {
                if (parents.size() == level) parents.add(key); else parents.set(level, key);
            } else {
                List<String> path = new ArrayList<>(parents);
                path.add(key);
                addConfigurationProperty(root, file, String.join(".", path), value.replace("\"", "").replace("'", ""),
                        line, evidence, observations);
            }
        }
    }

    private static void addConfigurationProperty(Path root, Path file, String key, String value, int line,
                                                 List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        boolean dynamic = SpringDiscoverySupport.dynamic(value);
        DiscoveryEvidence property = SpringDiscoverySupport.fileEvidence(ID, "spring-configuration-property", root, file,
                SpringDiscoverySupport.relative(root, file) + ":" + key, line, dynamic
                        ? DiscoveryConfidence.inferred(0.7, "Configuration value contains a dynamic expression.")
                        : DiscoveryConfidence.observedFact("Configuration property is explicitly declared."),
                SpringDiscoverySupport.details("configurationKey", key, "configurationValue", value,
                        "frameworkMarker", key, "uncertainty", dynamic ? "dynamic-expression" : ""));
        evidence.add(property);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-configuration-property-declared",
                "Spring configuration property " + key + " is declared in " + SpringDiscoverySupport.relative(root, file) + ".", property));
    }

    private static void addProfiles(SpringDiscoverySupport.Source source, SpringDiscoverySupport.AnnotationUse annotation,
                                    String origin, List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        for (String profile : SpringDiscoverySupport.stringValues(annotation.arguments())) {
            boolean dynamic = SpringDiscoverySupport.dynamic(profile);
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-profile", source,
                    source.qualifiedName() + ":" + profile, annotation.line(), dynamic
                            ? DiscoveryConfidence.inferred(0.7, "Profile contains a dynamic expression.")
                            : DiscoveryConfidence.observedFact("Spring profile is statically declared."), !dynamic,
                    SpringDiscoverySupport.details("annotation", "Profile", "frameworkMarker", "@Profile", "profile", profile,
                            "profileOrigin", origin, "uncertainty", dynamic ? "dynamic-expression" : ""));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-profile-declared",
                    "Spring profile " + profile + " is declared for " + source.className() + ".", item));
        }
    }

    private static void addFileProfile(Path root, Path file, String profile, String origin,
                                       List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations, boolean dynamic) {
        if (profile.isBlank()) return;
        DiscoveryConfidence confidence = dynamic ? DiscoveryConfidence.inferred(0.7, "Profile contains a dynamic expression.")
                : DiscoveryConfidence.observedFact("Spring profile is explicitly configured.");
        DiscoveryEvidence item = SpringDiscoverySupport.fileEvidence(ID, "spring-profile", root, file,
                SpringDiscoverySupport.relative(root, file) + ":" + profile, confidence,
                SpringDiscoverySupport.details("profile", profile, "profileOrigin", origin, "frameworkMarker", origin,
                        "uncertainty", dynamic ? "dynamic-expression" : ""));
        evidence.add(item);
        observations.add(SpringDiscoverySupport.observation(ID, "spring-profile-declared",
                "Spring profile " + profile + " is declared in " + SpringDiscoverySupport.relative(root, file) + ".", item));
    }
}
