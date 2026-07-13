package com.architectureworkbench.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Shared deterministic file, structured-document, provenance, and identity support for v0.2.4. */
final class ContractDiscoverySupport {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> EXCLUDED = List.of("/.git/", "/target/", "/build/", "/out/", "/node_modules/");
    private static final Pattern SEMVER = Pattern.compile("(?<![\\w])v?(\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?)(?![\\w])");
    private static final Pattern PATH_VERSION = Pattern.compile("(?:^|[/_.-])v(\\d+(?:\\.\\d+){0,2})(?:[/_.-]|$)", Pattern.CASE_INSENSITIVE);

    private ContractDiscoverySupport() { }

    static List<Path> files(Path root, List<String> diagnostics) {
        if (!Files.isDirectory(root)) {
            diagnostics.add("Repository root is not a directory: " + root);
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).filter(path -> !excluded(root, path))
                    .sorted(Comparator.comparing(path -> relative(root, path))).toList();
        } catch (IOException exception) {
            diagnostics.add("Unable to scan contract files: " + exception.getMessage());
            return List.of();
        }
    }

    static Optional<Document> document(Path root, Path path) {
        try {
            String content = Files.readString(path);
            String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
            ObjectMapper mapper = lower.endsWith(".json") || lower.endsWith(".avsc") ? JSON : YAML;
            try {
                return Optional.of(new Document(path, relative(root, path), SpringDiscoverySupport.moduleFor(root, path),
                        content, mapper.readTree(content), ""));
            } catch (Exception parseError) {
                return Optional.of(new Document(path, relative(root, path), SpringDiscoverySupport.moduleFor(root, path),
                        content, null, concise(parseError)));
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    static boolean structured(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".avsc");
    }

    static boolean openApiCandidate(Document document) {
        String name = document.path().toLowerCase(Locale.ROOT);
        return name.contains("openapi") || name.contains("swagger")
                || Pattern.compile("(?m)^\\s*(?:openapi|swagger)\\s*[:=]").matcher(document.content()).find();
    }

    static boolean asyncApiCandidate(Document document) {
        return document.path().toLowerCase(Locale.ROOT).contains("asyncapi")
                || Pattern.compile("(?m)^\\s*asyncapi\\s*:").matcher(document.content()).find();
    }

    static boolean eventSchemaCandidate(Document document) {
        String value = (document.path() + "\n" + document.content()).toLowerCase(Locale.ROOT);
        return document.path().toLowerCase(Locale.ROOT).endsWith(".avsc") || value.contains("eventtype")
                || value.contains("detail-type") || value.contains("x-event-type")
                || pathSegment(document.path(), "event") || pathSegment(document.path(), "events")
                || fileStem(document.path()).toLowerCase(Locale.ROOT).endsWith("event");
    }

    static boolean commandSchemaCandidate(Document document) {
        String value = (document.path() + "\n" + document.content()).toLowerCase(Locale.ROOT);
        return value.contains("commandtype") || value.contains("command-type") || value.contains("x-command-type")
                || pathSegment(document.path(), "command") || pathSegment(document.path(), "commands")
                || fileStem(document.path()).toLowerCase(Locale.ROOT).endsWith("command");
    }

    static DiscoveryEvidence evidence(DiscoveryPluginId pluginId, String type, Path root, Path file, String identity,
                                      int line, DiscoveryConfidence confidence, boolean observed,
                                      List<String> sourceEvidenceIds, Map<String, String> details) {
        String path = relative(root, file);
        Map<String, String> attributes = base(pluginId, path, SpringDiscoverySupport.moduleFor(root, file), line, observed);
        attributes.putAll(details);
        attributes.putIfAbsent("symbol", identity);
        attributes.putIfAbsent("configurationLocation", path + ":" + Math.max(1, line));
        attributes.putIfAbsent("uncertainty", "");
        attributes.putIfAbsent("sourceEvidenceIds", String.join(",", sourceEvidenceIds));
        List<String> references = new ArrayList<>();
        references.add(path);
        references.addAll(sourceEvidenceIds);
        String key = type + "|" + path + "|" + identity + "|" + line;
        return new DiscoveryEvidence(DiscoveryIdentity.evidenceId(pluginId, key), type, pluginId.value(),
                "path:" + path + ":line:" + Math.max(1, line), identity, confidence, observed, Instant.EPOCH,
                references, Map.copyOf(attributes));
    }

    static DiscoveryEvidence sourceEvidence(DiscoveryPluginId pluginId, String type, SpringDiscoverySupport.Source source,
                                            String identity, int line, DiscoveryConfidence confidence, boolean observed,
                                            List<String> sourceEvidenceIds, Map<String, String> details) {
        Map<String, String> attributes = base(pluginId, source.path(), source.module(), line, observed);
        attributes.put("packageName", source.packageName());
        attributes.put("className", source.className());
        attributes.putAll(details);
        attributes.putIfAbsent("symbol", identity);
        attributes.putIfAbsent("configurationLocation", source.path() + ":" + Math.max(1, line));
        attributes.putIfAbsent("uncertainty", "");
        attributes.putIfAbsent("sourceEvidenceIds", String.join(",", sourceEvidenceIds));
        List<String> references = new ArrayList<>();
        references.add(source.path());
        references.addAll(sourceEvidenceIds);
        return new DiscoveryEvidence(DiscoveryIdentity.evidenceId(pluginId, type + "|" + source.path() + "|" + identity + "|" + line),
                type, pluginId.value(), "path:" + source.path() + ":line:" + Math.max(1, line), identity,
                confidence, observed, Instant.EPOCH, references, Map.copyOf(attributes));
    }

    static DiscoveryEvidence parseError(DiscoveryPluginId pluginId, Path root, Document document, String format) {
        return evidence(pluginId, "contract-parse-error", root, document.absolutePath(), document.path(), 1,
                DiscoveryConfidence.inferred(0.2, "The candidate contract file exists but parsing failed."), true, List.of(),
                details("format", format, "errorCode", "INVALID_CONTRACT_DOCUMENT", "errorMessage", document.parseError(),
                        "recoverable", "true", "symbol", document.path()));
    }

    static DiscoveryObservation observation(DiscoveryPluginId pluginId, String type, String description,
                                            DiscoveryConfidence confidence, List<DiscoveryEvidence> evidence) {
        List<String> ids = evidence.stream().map(DiscoveryEvidence::evidenceId).toList();
        return new DiscoveryObservation(DiscoveryIdentity.observationId(pluginId, type + "|" + String.join("|", ids)),
                type, description, confidence, ids, Instant.EPOCH);
    }

    static DiscoveryPluginResult result(DiscoveryPluginId id, Instant started, List<DiscoveryEvidence> evidence,
                                        List<DiscoveryObservation> observations, List<String> diagnostics, boolean partial) {
        DiscoveryOutput output = new DiscoveryOutput(evidence, observations, diagnostics);
        return partial
                ? new DiscoveryPluginResult(id, DiscoveryPluginStatus.PARTIAL_SUCCESS, output, Duration.between(started, Instant.now()),
                    "One or more candidate contract files could not be fully parsed; valid evidence was preserved.")
                : DiscoveryPluginResult.succeeded(id, output, Duration.between(started, Instant.now()));
    }

    static List<DiscoveryEvidence> priorEvidence(DiscoveryInput input) {
        return input.priorOutputs().stream().flatMap(output -> output.evidence().stream()).toList();
    }

    static List<DiscoveryEvidence> priorEvidence(DiscoveryInput input, String... types) {
        List<String> accepted = List.of(types);
        return priorEvidence(input).stream().filter(item -> accepted.contains(item.evidenceType())).toList();
    }

    static Map<String, String> details(String... pairs) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        return values;
    }

    static String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            if (current == null) return "";
            current = current.path(segment);
        }
        if (current == null || current.isMissingNode() || current.isNull() || current.isContainerNode()) return "";
        return current.asText("");
    }

    static List<Map.Entry<String, JsonNode>> fields(JsonNode node) {
        if (node == null || !node.isObject()) return List.of();
        List<Map.Entry<String, JsonNode>> result = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        fields.forEachRemaining(result::add);
        return List.copyOf(result);
    }

    static List<String> schemaReferences(JsonNode node) {
        List<String> result = new ArrayList<>();
        collectSchemaReferences(node, result);
        return result.stream().distinct().sorted().toList();
    }

    private static void collectSchemaReferences(JsonNode node, List<String> result) {
        if (node == null) return;
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if (entry.getKey().equals("$ref") && entry.getValue().isTextual())
                    result.add(entry.getValue().asText().replaceFirst("^.*/", ""));
                else collectSchemaReferences(entry.getValue(), result);
            });
        } else if (node.isArray()) node.forEach(child -> collectSchemaReferences(child, result));
    }

    static String explicitVersion(JsonNode root) {
        for (String key : List.of("version", "schemaVersion", "schema-version", "x-version", "apiVersion")) {
            String value = text(root, key);
            if (!value.isBlank()) return value;
        }
        String info = text(root, "info", "version");
        return info;
    }

    static String pathVersion(String path) {
        Matcher matcher = PATH_VERSION.matcher(path);
        return matcher.find() ? matcher.group(1) : "";
    }

    static String semanticVersion(String value) {
        Matcher matcher = SEMVER.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group(1) : "";
    }

    static int line(String content, String marker) {
        if (marker == null || marker.isBlank()) return 1;
        int offset = content.indexOf(marker);
        if (offset < 0) return 1;
        int line = 1;
        for (int i = 0; i < offset; i++) if (content.charAt(i) == '\n') line++;
        return line;
    }

    static String fileStem(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.indexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    static boolean dynamic(String value) {
        return value != null && (value.contains("${") || value.contains("#{") || value.contains("+") || value.contains("System.getenv"));
    }

    static String relative(Path root, Path path) { return SpringDiscoverySupport.relative(root, path); }

    private static Map<String, String> base(DiscoveryPluginId pluginId, String path, String module, int line, boolean observed) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("filePath", path);
        attributes.put("module", module);
        attributes.put("packageName", "");
        attributes.put("className", "");
        attributes.put("line", Integer.toString(Math.max(1, line)));
        attributes.put("pluginId", pluginId.value());
        attributes.put("classification", observed ? "observed" : "inferred");
        return attributes;
    }

    private static boolean pathSegment(String path, String segment) {
        return ("/" + path.toLowerCase(Locale.ROOT) + "/").contains("/" + segment + "/");
    }

    private static boolean excluded(Path root, Path path) {
        String value = "/" + relative(root, path) + "/";
        return EXCLUDED.stream().anyMatch(value::contains);
    }

    private static String concise(Exception exception) {
        String value = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        int newline = value.indexOf('\n');
        return newline < 0 ? value : value.substring(0, newline);
    }

    record Document(Path absolutePath, String path, String module, String content, JsonNode root, String parseError) {
        boolean valid() { return root != null; }
    }
}
