package com.architectureworkbench.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Shared, deliberately lexical support for deterministic Spring discovery. */
final class SpringDiscoverySupport {
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;?");
    private static final Pattern TYPE = Pattern.compile("(?s)((?:\\s*@(?:[\\w$.]+)(?:\\s*\\([^)]*\\))?\\s*)*)(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|sealed\\s+|non-sealed\\s+|static\\s+)*(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)([^\\{;]*)\\{");
    private static final Pattern METHOD = Pattern.compile("(?m)((?:^[ \\t]*@(?:[\\w$.]+)(?:[ \\t]*\\([^)]*\\))?[ \\t]*(?:\\R|$))*)^[ \\t]*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default)[ \\t]+)*([A-Za-z_$][\\w$<>?,. \\[\\]]*)[ \\t]+([A-Za-z_$][\\w$]*)[ \\t]*\\(([^)]*)\\)[ \\t]*(?:throws[^\\{;]+)?[\\{;]");
    private static final Pattern CONSTRUCTOR = Pattern.compile("(?m)((?:^[ \\t]*@(?:[\\w$.]+)(?:[ \\t]*\\([^)]*\\))?[ \\t]*(?:\\R|$))*)^[ \\t]*(?:(?:public|protected|private)[ \\t]+)?([A-Za-z_$][\\w$]*)[ \\t]*\\(([^)]*)\\)[ \\t]*(?:throws[^\\{;]+)?\\{");
    private static final Pattern FIELD = Pattern.compile("(?m)((?:^[ \\t]*@(?:[\\w$.]+)(?:[ \\t]*\\([^)]*\\))?[ \\t]*(?:\\R|$))+)[ \\t]*(?:(?:public|protected|private|static|final|transient|volatile)[ \\t]+)*([A-Za-z_$][\\w$<>?,. \\[\\]]*)[ \\t]+([A-Za-z_$][\\w$]*)[ \\t]*(?:=[^;]*)?;");
    private static final Pattern INLINE_METHOD = Pattern.compile("(?m)^[ \\t]*(@(?:[\\w$.]+)(?:[ \\t]*\\([^)]*\\))?)[ \\t]+(?:(?:public|protected|private|static|final|abstract|synchronized|default)[ \\t]+)*([A-Za-z_$][\\w$<>?,. \\[\\]]*)[ \\t]+([A-Za-z_$][\\w$]*)[ \\t]*\\(([^)]*)\\)[ \\t]*(?:throws[^\\{;]+)?[\\{;]");
    private static final Pattern INLINE_FIELD = Pattern.compile("(?m)^[ \\t]*(@(?:[\\w$.]+)(?:[ \\t]*\\([^)]*\\))?)[ \\t]+(?:(?:public|protected|private|static|final|transient|volatile)[ \\t]+)*([A-Za-z_$][\\w$<>?,. \\[\\]]*)[ \\t]+([A-Za-z_$][\\w$]*)[ \\t]*(?:=[^;]*)?;");
    private static final Pattern ANNOTATION = Pattern.compile("@([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)(?:\\s*\\(([^)]*)\\))?");
    private static final Pattern PARAMETER = Pattern.compile("(?:@[\\w$.]+(?:\\s*\\([^)]*\\))?\\s*)*(?:final\\s+)?([A-Za-z_$][\\w$<>?,.\\[\\]]*)\\s+([A-Za-z_$][\\w$]*)");

    private SpringDiscoverySupport() {
    }

    static List<Source> sources(Path root, List<String> diagnostics) {
        if (!Files.isDirectory(root)) {
            diagnostics.add("Repository root is not a directory: " + root);
            return List.of();
        }
        List<Path> paths;
        try (Stream<Path> stream = Files.walk(root)) {
            paths = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !excluded(root, path))
                    .sorted(Comparator.comparing(path -> relative(root, path)))
                    .toList();
        } catch (IOException exception) {
            diagnostics.add("Unable to scan Java sources: " + exception.getMessage());
            return List.of();
        }
        List<Source> result = new ArrayList<>();
        for (Path path : paths) {
            try {
                result.add(parse(root, path, Files.readString(path)));
            } catch (IOException | RuntimeException exception) {
                diagnostics.add("Unable to inspect " + relative(root, path) + ": " + exception.getMessage());
            }
        }
        return List.copyOf(result);
    }

    static List<Path> configurationFiles(Path root, List<String> diagnostics) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> isSpringConfiguration(path.getFileName().toString()))
                    .filter(path -> !excluded(root, path))
                    .sorted(Comparator.comparing(path -> relative(root, path)))
                    .toList();
        } catch (IOException exception) {
            diagnostics.add("Unable to scan Spring configuration: " + exception.getMessage());
            return List.of();
        }
    }

    private static Source parse(Path root, Path path, String raw) {
        String content = stripComments(raw);
        Matcher packageMatcher = PACKAGE.matcher(content);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
        Matcher typeMatcher = TYPE.matcher(content);
        String annotations = "";
        String kind = "";
        String className = path.getFileName().toString().replaceFirst("\\.java$", "");
        String header = "";
        int typeStart = 0;
        int annotationStart = 0;
        if (typeMatcher.find()) {
            annotations = typeMatcher.group(1);
            kind = typeMatcher.group(2);
            className = typeMatcher.group(3);
            header = typeMatcher.group(4);
            typeStart = typeMatcher.start(2);
            annotationStart = typeMatcher.start(1);
        }
        List<Method> methods = new ArrayList<>();
        Matcher methodMatcher = METHOD.matcher(content);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(3);
            String returnType = cleanType(methodMatcher.group(2));
            if (!methodName.equals(className) && !List.of("public", "protected", "private").contains(returnType))
                methods.add(new Method(methodName, returnType, methodMatcher.group(4),
                        annotations(methodMatcher.group(1), content, methodMatcher.start()), line(content, methodMatcher.start(3)), methodMatcher.start()));
        }
        Matcher inlineMethod = INLINE_METHOD.matcher(content);
        while (inlineMethod.find()) {
            if (methods.stream().noneMatch(method -> method.offset() == inlineMethod.start()))
                methods.add(new Method(inlineMethod.group(3), cleanType(inlineMethod.group(2)), inlineMethod.group(4),
                        annotations(inlineMethod.group(1), content, inlineMethod.start()), line(content, inlineMethod.start(3)), inlineMethod.start()));
        }
        Matcher constructorMatcher = CONSTRUCTOR.matcher(content);
        while (constructorMatcher.find()) {
            if (constructorMatcher.group(2).equals(className)) {
                methods.add(new Method(className, "", constructorMatcher.group(3),
                        annotations(constructorMatcher.group(1), content, constructorMatcher.start()), line(content, constructorMatcher.start(2)), constructorMatcher.start()));
            }
        }
        methods.sort(Comparator.comparingInt(Method::offset));
        List<Field> fields = new ArrayList<>();
        Matcher fieldMatcher = FIELD.matcher(content);
        while (fieldMatcher.find()) {
            fields.add(new Field(fieldMatcher.group(3), cleanType(fieldMatcher.group(2)),
                    annotations(fieldMatcher.group(1), content, fieldMatcher.start()), line(content, fieldMatcher.start(3))));
        }
        Matcher inlineField = INLINE_FIELD.matcher(content);
        while (inlineField.find()) {
            if (fields.stream().noneMatch(field -> field.line() == line(content, inlineField.start(3)) && field.name().equals(inlineField.group(3))))
                fields.add(new Field(inlineField.group(3), cleanType(inlineField.group(2)),
                        annotations(inlineField.group(1), content, inlineField.start()), line(content, inlineField.start(3))));
        }
        return new Source(path, relative(root, path), moduleFor(root, path), packageName, className, kind, header,
                content, annotations(annotations, content, Math.max(0, annotationStart)), line(content, Math.max(0, typeStart)),
                List.copyOf(methods), List.copyOf(fields));
    }

    static List<AnnotationUse> annotations(String block, String wholeContent, int baseOffset) {
        List<AnnotationUse> result = new ArrayList<>();
        Matcher matcher = ANNOTATION.matcher(block == null ? "" : block);
        while (matcher.find()) {
            String qualified = matcher.group(1);
            String name = qualified.substring(qualified.lastIndexOf('.') + 1);
            int offset = Math.max(0, baseOffset + matcher.start());
            result.add(new AnnotationUse(name, matcher.group(2) == null ? "" : matcher.group(2).trim(), line(wholeContent, offset), offset));
        }
        return List.copyOf(result);
    }

    static Optional<AnnotationUse> annotation(List<AnnotationUse> annotations, String name) {
        return annotations.stream().filter(annotation -> annotation.name().equals(name)).findFirst();
    }

    static List<Parameter> parameters(String source) {
        List<Parameter> result = new ArrayList<>();
        for (String value : splitTopLevel(source, ',')) {
            Matcher matcher = PARAMETER.matcher(value.trim());
            if (matcher.matches()) result.add(new Parameter(cleanType(matcher.group(1)), matcher.group(2)));
        }
        return List.copyOf(result);
    }

    static List<String> stringValues(String arguments) {
        List<String> values = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\\"([^\\\"]*)\\\"").matcher(arguments == null ? "" : arguments);
        while (matcher.find()) values.add(matcher.group(1));
        return List.copyOf(values);
    }

    static String namedValue(String arguments, String name) {
        Matcher matcher = Pattern.compile("(?:^|,)\\s*" + Pattern.quote(name) + "\\s*=\\s*([^,}]+(?:\\{[^}]*})?)").matcher(arguments == null ? "" : arguments);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    static String firstString(String arguments) {
        List<String> values = stringValues(arguments);
        return values.isEmpty() ? "" : values.get(0);
    }

    static String joinPath(String classPath, String methodPath) {
        String combined = "/" + blankToEmpty(classPath) + "/" + blankToEmpty(methodPath);
        combined = combined.replaceAll("/{2,}", "/");
        if (combined.length() > 1 && combined.endsWith("/")) combined = combined.substring(0, combined.length() - 1);
        return combined;
    }

    static String simpleType(String type) {
        String value = cleanType(type).replace("...", "");
        int generic = value.indexOf('<');
        if (generic >= 0) value = value.substring(0, generic);
        int dot = value.lastIndexOf('.');
        return dot >= 0 ? value.substring(dot + 1) : value;
    }

    static List<String> genericTypes(String type) {
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start < 0 || end <= start) return List.of();
        return splitTopLevel(type.substring(start + 1, end), ',').stream().map(String::trim).map(SpringDiscoverySupport::simpleType).toList();
    }

    static String relative(Path root, Path path) {
        return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    static String moduleFor(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path current = Files.isDirectory(path) ? path : path.getParent();
        while (current != null && current.toAbsolutePath().normalize().startsWith(normalizedRoot)) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                String module = relative(normalizedRoot, current);
                return module.isBlank() ? "." : module;
            }
            current = current.getParent();
        }
        return ".";
    }

    static DiscoveryEvidence evidence(DiscoveryPluginId pluginId, String type, Source source, String identity, int line,
                                      DiscoveryConfidence confidence, boolean observed, Map<String, String> details) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("filePath", source.path());
        attributes.put("module", source.module());
        attributes.put("packageName", source.packageName());
        attributes.put("className", source.className());
        attributes.put("symbol", source.className());
        attributes.put("line", Integer.toString(Math.max(1, line)));
        attributes.put("pluginId", pluginId.value());
        attributes.put("classification", observed ? "observed" : "inferred");
        attributes.putAll(details);
        attributes.putIfAbsent("annotation", "");
        attributes.putIfAbsent("frameworkMarker", "");
        attributes.putIfAbsent("methodName", "");
        attributes.putIfAbsent("uncertainty", "");
        String key = type + "|" + source.path() + "|" + identity + "|" + line;
        return new DiscoveryEvidence(DiscoveryIdentity.evidenceId(pluginId, key), type, pluginId.value(),
                "path:" + source.path() + ":line:" + Math.max(1, line), identity, confidence, observed, Instant.EPOCH,
                List.of(source.path()), Map.copyOf(attributes));
    }

    static DiscoveryEvidence fileEvidence(DiscoveryPluginId pluginId, String type, Path root, Path file, String identity,
                                          DiscoveryConfidence confidence, Map<String, String> details) {
        return fileEvidence(pluginId, type, root, file, identity, 1, confidence, details);
    }

    static DiscoveryEvidence fileEvidence(DiscoveryPluginId pluginId, String type, Path root, Path file, String identity, int line,
                                          DiscoveryConfidence confidence, Map<String, String> details) {
        String path = relative(root, file);
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("filePath", path);
        attributes.put("module", moduleFor(root, file));
        attributes.put("packageName", "");
        attributes.put("className", "");
        attributes.put("symbol", path);
        attributes.put("line", Integer.toString(Math.max(1, line)));
        attributes.put("pluginId", pluginId.value());
        boolean observed = !details.containsKey("uncertainty") || details.get("uncertainty").isBlank();
        attributes.put("classification", observed ? "observed" : "inferred");
        attributes.putAll(details);
        attributes.putIfAbsent("annotation", "");
        attributes.putIfAbsent("frameworkMarker", "");
        attributes.putIfAbsent("methodName", "");
        attributes.putIfAbsent("uncertainty", "");
        return new DiscoveryEvidence(DiscoveryIdentity.evidenceId(pluginId, type + "|" + path + "|" + identity), type,
                pluginId.value(), "path:" + path + ":line:" + Math.max(1, line), identity, confidence,
                observed, Instant.EPOCH,
                List.of(path), Map.copyOf(attributes));
    }

    static DiscoveryObservation observation(DiscoveryPluginId pluginId, String type, String description, DiscoveryEvidence evidence) {
        return new DiscoveryObservation(DiscoveryIdentity.observationId(pluginId, type + "|" + evidence.evidenceId()), type,
                description, evidence.confidence(), List.of(evidence.evidenceId()), Instant.EPOCH);
    }

    static Map<String, String> details(String... pairs) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) values.put(pairs[i], blankToEmpty(pairs[i + 1]));
        return values;
    }

    static boolean dynamic(String value) {
        return value.contains("${") || value.contains("#{") || value.contains("+");
    }

    private static boolean isSpringConfiguration(String name) {
        return name.matches("(?:application|bootstrap)(?:-[^.]+)?\\.(?:properties|ya?ml)");
    }

    private static boolean excluded(Path root, Path path) {
        String value = "/" + relative(root, path) + "/";
        return List.of("/.git/", "/target/", "/build/", "/out/", "/node_modules/").stream().anyMatch(value::contains);
    }

    private static String stripComments(String source) {
        StringBuilder result = new StringBuilder(source);
        boolean line = false, block = false, string = false, character = false, escape = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i), next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (line) {
                if (current == '\n') line = false; else result.setCharAt(i, ' ');
            } else if (block) {
                if (current == '*' && next == '/') { result.setCharAt(i, ' '); result.setCharAt(++i, ' '); block = false; }
                else if (current != '\n' && current != '\r') result.setCharAt(i, ' ');
            } else if (!string && !character && current == '/' && next == '/') {
                result.setCharAt(i, ' '); result.setCharAt(++i, ' '); line = true;
            } else if (!string && !character && current == '/' && next == '*') {
                result.setCharAt(i, ' '); result.setCharAt(++i, ' '); block = true;
            } else {
                if (!escape && current == '"' && !character) string = !string;
                if (!escape && current == '\'' && !string) character = !character;
                escape = (string || character) && current == '\\' && !escape;
                if (current != '\\') escape = false;
            }
        }
        return result.toString();
    }

    private static int line(String source, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, source.length()); i++) if (source.charAt(i) == '\n') line++;
        return line;
    }

    private static String cleanType(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static List<String> splitTopLevel(String source, char separator) {
        List<String> values = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '<' || c == '{' || c == '(') depth++;
            else if (c == '>' || c == '}' || c == ')') depth--;
            else if (c == separator && depth == 0) { values.add(source.substring(start, i)); start = i + 1; }
        }
        values.add(source.substring(start));
        return values;
    }

    private static String blankToEmpty(String value) { return value == null ? "" : value; }

    record Source(Path absolutePath, String path, String module, String packageName, String className, String kind,
                  String header, String content, List<AnnotationUse> annotations, int typeLine,
                  List<Method> methods, List<Field> fields) {
        String qualifiedName() { return packageName.isBlank() ? className : packageName + "." + className; }
    }

    record AnnotationUse(String name, String arguments, int line, int offset) { }
    record Method(String name, String returnType, String parameters, List<AnnotationUse> annotations, int line, int offset) {
        String symbol(Source source) { return source.className() + "." + name; }
    }
    record Field(String name, String type, List<AnnotationUse> annotations, int line) { }
    record Parameter(String type, String name) { }
}
