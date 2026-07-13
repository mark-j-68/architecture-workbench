package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Deterministically extracts directly observable Java source structure without
 * attempting symbol solving or framework interpretation.
 */
public class JavaStructureDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("java.structure");

    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;?");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+(static\\s+)?([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$*][\\w$*]*)+)\\s*;?");
    private static final Pattern TYPE = Pattern.compile("(?<![\\w$])(@interface|class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)\\s*([^\\{;]*)");
    private static final Pattern ANNOTATION = Pattern.compile("@([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)");
    private static final Pattern EXTENDS = Pattern.compile("\\bextends\\s+([^\\s,{]+(?:\\s*,\\s*[^\\s,{]+)*)");
    private static final Pattern IMPLEMENTS = Pattern.compile("\\bimplements\\s+([^\\{]+)");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(
                ID,
                "Java Structure Discovery Plugin",
                "0.2.2",
                "Language Plugin",
                List.of("java"),
                List.of(
                        DiscoveryPluginCapability.DETECT_JAVA_SOURCE_ROOTS,
                        DiscoveryPluginCapability.DETECT_JAVA_TYPES,
                        DiscoveryPluginCapability.DETECT_JAVA_IMPORTS
                ),
                List.of(
                        new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(MavenDiscoveryPlugin.ID, false)
                ),
                true
        );
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        if (!Files.isDirectory(input.rootDirectory())) {
            return DiscoveryPluginResult.failed(ID, Duration.between(started, Instant.now()),
                    "Repository root is not a directory: " + input.rootDirectory());
        }

        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        try {
            List<Path> sourceRoots = sourceRoots(input.rootDirectory());
            for (Path sourceRoot : sourceRoots) {
                addSourceRoot(input.rootDirectory(), sourceRoot, evidence, observations);
            }

            Map<String, DiscoveryEvidence> packages = new LinkedHashMap<>();
            for (Path javaFile : javaFiles(input.rootDirectory())) {
                try {
                    inspectJavaFile(input.rootDirectory(), javaFile, sourceRoots, packages, evidence, observations);
                } catch (IOException | RuntimeException exception) {
                    diagnostics.add("Unable to inspect " + relative(input.rootDirectory(), javaFile) + ": " + exception.getMessage());
                }
            }
            return DiscoveryPluginResult.succeeded(
                    ID,
                    new DiscoveryOutput(evidence, observations, diagnostics),
                    Duration.between(started, Instant.now())
            );
        } catch (IOException exception) {
            return DiscoveryPluginResult.failed(ID, Duration.between(started, Instant.now()),
                    "Unable to scan Java sources: " + exception.getMessage());
        }
    }

    private static void addSourceRoot(
            Path repositoryRoot,
            Path sourceRoot,
            List<DiscoveryEvidence> evidence,
            List<DiscoveryObservation> observations
    ) {
        String path = relative(repositoryRoot, sourceRoot);
        boolean test = isTestRoot(path);
        String module = moduleFor(repositoryRoot, sourceRoot);
        String type = test ? "java-test-source-root" : "java-source-root";
        Map<String, String> attributes = attributes(
                "filePath", path,
                "sourceRoot", path,
                "sourceSet", test ? "test" : "main",
                "module", module,
                "pluginId", ID.value()
        );
        DiscoveryEvidence rootEvidence = evidence(type, path, path, 1,
                DiscoveryConfidence.observedFact("Java source root directory exists."), true, attributes);
        evidence.add(rootEvidence);
        observations.add(observation(
                test ? "java-test-source-root-detected" : "java-source-root-detected",
                (test ? "Java test source root" : "Java source root") + " detected: " + path,
                rootEvidence
        ));
    }

    private static void inspectJavaFile(
            Path repositoryRoot,
            Path javaFile,
            List<Path> sourceRoots,
            Map<String, DiscoveryEvidence> packages,
            List<DiscoveryEvidence> evidence,
            List<DiscoveryObservation> observations
    ) throws IOException {
        String raw = Files.readString(javaFile);
        String content = maskCommentsAndLiterals(raw);
        String filePath = relative(repositoryRoot, javaFile);
        Path sourceRootPath = containingSourceRoot(javaFile, sourceRoots).orElse(javaFile.getParent());
        String sourceRoot = relative(repositoryRoot, sourceRootPath);
        String sourceSet = isTestRoot(sourceRoot) ? "test" : "main";
        String module = moduleFor(repositoryRoot, javaFile);

        Matcher packageMatcher = PACKAGE.matcher(content);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
        if (!packageName.isBlank()) {
            int line = lineNumber(content, packageMatcher.start(1));
            String packageKey = sourceRoot + "|" + packageName;
            if (!packages.containsKey(packageKey)) {
                DiscoveryEvidence packageEvidence = evidence(
                        "java-package",
                        filePath,
                        "java:" + packageName + ":" + sourceRoot,
                        line,
                        DiscoveryConfidence.observedFact("Java package declaration is present in source."),
                        true,
                        attributes(
                                "filePath", filePath,
                                "line", Integer.toString(line),
                                "packageName", packageName,
                                "sourceRoot", sourceRoot,
                                "sourceSet", sourceSet,
                                "module", module,
                                "pluginId", ID.value()
                        )
                );
                packages.put(packageKey, packageEvidence);
                evidence.add(packageEvidence);
                observations.add(observation("java-package-declared",
                        "Java package " + packageName + " is declared in module " + module + ".", packageEvidence));
            }
        }

        List<TypeDeclaration> declarations = typeDeclarations(content, packageName);
        for (TypeDeclaration declaration : declarations) {
            String typeEvidenceName = switch (declaration.kind()) {
                case "class" -> "java-class";
                case "interface" -> "java-interface";
                case "enum" -> "java-enum";
                case "record" -> "java-record";
                case "annotation" -> "java-annotation-declaration";
                default -> throw new IllegalStateException("Unexpected Java type: " + declaration.kind());
            };
            DiscoveryEvidence typeEvidence = evidence(
                    typeEvidenceName,
                    filePath,
                    declaration.qualifiedName(),
                    declaration.line(),
                    DiscoveryConfidence.observedFact("Java " + declaration.kind() + " declaration is present in source."),
                    true,
                    attributes(
                            "filePath", filePath,
                            "line", Integer.toString(declaration.line()),
                            "packageName", packageName,
                            "className", declaration.name(),
                            "qualifiedName", declaration.qualifiedName(),
                            "typeKind", declaration.kind(),
                            "sourceRoot", sourceRoot,
                            "sourceSet", sourceSet,
                            "module", module,
                            "pluginId", ID.value()
                    )
            );
            evidence.add(typeEvidence);
            observations.add(observation("java-" + declaration.kind() + "-declared",
                    "Java " + declaration.kind() + " declared: " + declaration.qualifiedName(), typeEvidence));

            for (String parent : declaration.extendsTypes()) {
                addTypeRelationship("java-inheritance", "extends", declaration, parent, filePath, packageName,
                        sourceRoot, sourceSet, module, evidence, observations);
            }
            for (String implemented : declaration.implementsTypes()) {
                addTypeRelationship("java-implementation", "implements", declaration, implemented, filePath, packageName,
                        sourceRoot, sourceSet, module, evidence, observations);
            }
        }

        Matcher importMatcher = IMPORT.matcher(content);
        while (importMatcher.find()) {
            boolean staticImport = importMatcher.group(1) != null;
            String importName = importMatcher.group(2);
            int line = lineNumber(content, importMatcher.start(2));
            String importedPackage = importedPackageCandidate(importName, staticImport);
            DiscoveryEvidence importEvidence = evidence(
                    "java-import",
                    filePath,
                    filePath + ":" + line + ":" + importName,
                    line,
                    DiscoveryConfidence.observedFact("Java import declaration is present in source."),
                    true,
                    attributes(
                            "filePath", filePath,
                            "line", Integer.toString(line),
                            "packageName", packageName,
                            "className", declarations.isEmpty() ? "" : declarations.get(0).name(),
                            "importName", importName,
                            "importedPackage", importedPackage,
                            "static", Boolean.toString(staticImport),
                            "wildcard", Boolean.toString(importName.endsWith(".*")),
                            "sourceRoot", sourceRoot,
                            "sourceSet", sourceSet,
                            "module", module,
                            "pluginId", ID.value()
                    )
            );
            evidence.add(importEvidence);
            observations.add(observation("java-import-declared",
                    "Java source " + filePath + " imports " + importName + ".", importEvidence));
        }

        Matcher annotationMatcher = ANNOTATION.matcher(content);
        while (annotationMatcher.find()) {
            if (content.regionMatches(annotationMatcher.start(), "@interface", 0, "@interface".length())) {
                continue;
            }
            String annotationName = annotationMatcher.group(1);
            int line = lineNumber(content, annotationMatcher.start(1));
            TypeDeclaration owner = enclosingType(declarations, annotationMatcher.start());
            DiscoveryEvidence annotationEvidence = evidence(
                    "java-annotation",
                    filePath,
                    filePath + ":" + line + ":" + annotationName,
                    line,
                    DiscoveryConfidence.observedFact("Java annotation use is present in source."),
                    true,
                    attributes(
                            "filePath", filePath,
                            "line", Integer.toString(line),
                            "packageName", packageName,
                            "className", owner == null ? "" : owner.name(),
                            "annotatedElement", owner == null ? filePath : owner.qualifiedName(),
                            "annotationName", annotationName,
                            "sourceRoot", sourceRoot,
                            "sourceSet", sourceSet,
                            "module", module,
                            "pluginId", ID.value()
                    )
            );
            evidence.add(annotationEvidence);
            observations.add(observation("java-annotation-used",
                    "Java annotation @" + annotationName + " is used in " + filePath + ".", annotationEvidence));
        }
    }

    private static void addTypeRelationship(
            String evidenceType,
            String relationship,
            TypeDeclaration declaration,
            String target,
            String filePath,
            String packageName,
            String sourceRoot,
            String sourceSet,
            String module,
            List<DiscoveryEvidence> evidence,
            List<DiscoveryObservation> observations
    ) {
        DiscoveryEvidence relationshipEvidence = evidence(
                evidenceType,
                filePath,
                declaration.qualifiedName() + ":" + relationship + ":" + target,
                declaration.line(),
                DiscoveryConfidence.high("Relationship is parsed directly from a Java type declaration."),
                true,
                attributes(
                        "filePath", filePath,
                        "line", Integer.toString(declaration.line()),
                        "packageName", packageName,
                        "className", declaration.name(),
                        "sourceType", declaration.qualifiedName(),
                        "relationship", relationship,
                        "targetType", target,
                        "sourceRoot", sourceRoot,
                        "sourceSet", sourceSet,
                        "module", module,
                        "pluginId", ID.value()
                )
        );
        evidence.add(relationshipEvidence);
        observations.add(observation("java-type-" + relationship,
                declaration.qualifiedName() + " " + relationship + " " + target + ".", relationshipEvidence));
    }

    private static List<TypeDeclaration> typeDeclarations(String content, String packageName) {
        List<TypeDeclaration> declarations = new ArrayList<>();
        Matcher matcher = TYPE.matcher(content);
        while (matcher.find()) {
            String token = matcher.group(1);
            String kind = token.equals("@interface") ? "annotation" : token;
            String name = matcher.group(2);
            String qualifiedName = packageName.isBlank() ? name : packageName + "." + name;
            String tail = matcher.group(3);
            declarations.add(new TypeDeclaration(
                    name,
                    qualifiedName,
                    kind,
                    lineNumber(content, matcher.start(2)),
                    matcher.start(),
                    relationshipTypes(EXTENDS, tail),
                    relationshipTypes(IMPLEMENTS, tail)
            ));
        }
        return declarations;
    }

    private static List<String> relationshipTypes(Pattern pattern, String declarationTail) {
        Matcher matcher = pattern.matcher(declarationTail);
        if (!matcher.find()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String value : matcher.group(1).split(",")) {
            String cleaned = value.trim().replaceAll("\\s+", " ");
            if (!cleaned.isBlank()) {
                values.add(cleaned);
            }
        }
        return List.copyOf(values);
    }

    private static TypeDeclaration enclosingType(List<TypeDeclaration> declarations, int offset) {
        TypeDeclaration owner = null;
        for (TypeDeclaration declaration : declarations) {
            if (declaration.offset() <= offset) {
                owner = declaration;
            } else if (owner == null) {
                return declaration;
            } else {
                break;
            }
        }
        return owner;
    }

    private static String importedPackageCandidate(String importName, boolean staticImport) {
        String withoutWildcard = importName.endsWith(".*") ? importName.substring(0, importName.length() - 2) : importName;
        int segmentsToRemove = staticImport ? (importName.endsWith(".*") ? 1 : 2) : (importName.endsWith(".*") ? 0 : 1);
        String result = withoutWildcard;
        for (int i = 0; i < segmentsToRemove; i++) {
            int separator = result.lastIndexOf('.');
            if (separator < 0) {
                return "";
            }
            result = result.substring(0, separator);
        }
        return result;
    }

    private static List<Path> sourceRoots(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> isConventionalJavaRoot(relative(root, path)))
                    .filter(path -> !isExcluded(root, path))
                    .sorted(Comparator.comparing(path -> relative(root, path)))
                    .toList();
        }
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !isExcluded(root, path))
                    .sorted(Comparator.comparing(path -> relative(root, path)))
                    .toList();
        }
    }

    private static boolean isExcluded(Path root, Path path) {
        for (Path part : root.relativize(path)) {
            String value = part.toString();
            if (value.equals("target") || value.equals("build") || value.equals("out")
                    || value.equals("node_modules") || value.equals(".git")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConventionalJavaRoot(String path) {
        return path.equals("src/main/java") || path.endsWith("/src/main/java")
                || path.equals("src/test/java") || path.endsWith("/src/test/java");
    }

    private static boolean isTestRoot(String path) {
        return path.equals("src/test/java") || path.endsWith("/src/test/java");
    }

    private static Optional<Path> containingSourceRoot(Path file, List<Path> sourceRoots) {
        return sourceRoots.stream()
                .filter(file::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
    }

    static String moduleFor(Path repositoryRoot, Path path) {
        Path current = Files.isDirectory(path) ? path : path.getParent();
        while (current != null && current.startsWith(repositoryRoot)) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return relative(repositoryRoot, current);
            }
            if (current.equals(repositoryRoot)) {
                break;
            }
            current = current.getParent();
        }
        return ".";
    }

    private static int lineNumber(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String maskCommentsAndLiterals(String source) {
        StringBuilder masked = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        boolean string = false;
        boolean character = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    masked.append('\n');
                } else {
                    masked.append(' ');
                }
            } else if (blockComment) {
                if (current == '*' && next == '/') {
                    masked.append("  ");
                    i++;
                    blockComment = false;
                } else {
                    masked.append(current == '\n' ? '\n' : ' ');
                }
            } else if (string || character) {
                masked.append(current == '\n' ? '\n' : ' ');
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if ((string && current == '"') || (character && current == '\'')) {
                    string = false;
                    character = false;
                }
            } else if (current == '/' && next == '/') {
                masked.append("  ");
                i++;
                lineComment = true;
            } else if (current == '/' && next == '*') {
                masked.append("  ");
                i++;
                blockComment = true;
            } else if (current == '"') {
                masked.append(' ');
                string = true;
            } else if (current == '\'') {
                masked.append(' ');
                character = true;
            } else {
                masked.append(current);
            }
        }
        return masked.toString();
    }

    private static DiscoveryEvidence evidence(
            String type,
            String filePath,
            String identity,
            int line,
            DiscoveryConfidence confidence,
            boolean directlyObserved,
            Map<String, String> attributes
    ) {
        String stableIdentity = type + "|" + identity + "|" + filePath + "|" + line;
        return new DiscoveryEvidence(
                DiscoveryIdentity.stableId("java-evidence", stableIdentity),
                type,
                ID.value(),
                "path:" + filePath + (line > 0 ? ":line:" + line : ""),
                identity,
                confidence,
                directlyObserved,
                null,
                List.of(filePath),
                attributes
        );
    }

    private static DiscoveryObservation observation(String type, String description, DiscoveryEvidence evidence) {
        return new DiscoveryObservation(
                DiscoveryIdentity.stableId("java-observation", type, evidence.evidenceId()),
                type,
                description,
                evidence.confidence(),
                List.of(evidence.evidenceId()),
                null
        );
    }

    private static Map<String, String> attributes(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index], values[index + 1]);
        }
        return Map.copyOf(result);
    }

    static String relative(Path root, Path path) {
        if (root.equals(path)) {
            return ".";
        }
        return root.relativize(path).toString().replace('\\', '/');
    }

    private record TypeDeclaration(
            String name,
            String qualifiedName,
            String kind,
            int line,
            int offset,
            List<String> extendsTypes,
            List<String> implementsTypes
    ) {
    }
}
