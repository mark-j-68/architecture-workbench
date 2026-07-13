package com.architectureworkbench.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Discovers declared OpenAPI/Swagger contracts without generating or executing them. */
public class OpenApiContractDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("contract.openapi");
    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "OpenAPI Contract Discovery Plugin", "0.2.4", "Contract Plugin",
                List.of("openapi", "swagger", "yaml", "json"),
                List.of(DiscoveryPluginCapability.DETECT_OPENAPI_CONTRACTS),
                List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(SpringWebDiscoveryPlugin.ID, false)), true);
    }

    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        boolean partial = false;
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            if (!ContractDiscoverySupport.structured(file)) continue;
            var candidate = ContractDiscoverySupport.document(input.rootDirectory(), file);
            if (candidate.isEmpty() || !ContractDiscoverySupport.openApiCandidate(candidate.get())) continue;
            var document = candidate.get();
            String markerVersion = marker(document.content(), "openapi", "swagger");
            DiscoveryEvidence documentEvidence = ContractDiscoverySupport.evidence(ID, "openapi-document", input.rootDirectory(), file,
                    document.path(), ContractDiscoverySupport.line(document.content(), markerVersion),
                    DiscoveryConfidence.observedFact("An OpenAPI or Swagger document marker is explicitly declared."), true, List.of(),
                    ContractDiscoverySupport.details("contractId", document.path(), "contractType", ContractType.API.name(),
                            "openApiVersion", markerVersion, "format", extension(document.path())));
            evidence.add(documentEvidence);
            if (!document.valid()) {
                partial = true;
                DiscoveryEvidence error = ContractDiscoverySupport.parseError(ID, input.rootDirectory(), document, "openapi");
                evidence.add(error);
                observations.add(ContractDiscoverySupport.observation(ID, "openapi-document-partially-discovered",
                        "OpenAPI candidate " + document.path() + " was detected but could not be fully parsed.", error.confidence(), List.of(documentEvidence, error)));
                diagnostics.add("INVALID_CONTRACT_DOCUMENT|" + document.path() + "|" + document.parseError());
                continue;
            }
            inspect(input, document, documentEvidence, evidence, observations);
        }
        return ContractDiscoverySupport.result(ID, started, evidence, observations, diagnostics, partial);
    }

    private static void inspect(DiscoveryInput input, ContractDiscoverySupport.Document document, DiscoveryEvidence documentEvidence,
                                List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        JsonNode root = document.root();
        String version = first(ContractDiscoverySupport.text(root, "openapi"), ContractDiscoverySupport.text(root, "swagger"));
        String title = ContractDiscoverySupport.text(root, "info", "title");
        String apiVersion = ContractDiscoverySupport.text(root, "info", "version");
        DiscoveryEvidence metadata = ContractDiscoverySupport.evidence(ID, "api-contract", input.rootDirectory(), document.absolutePath(),
                document.path(), ContractDiscoverySupport.line(document.content(), title),
                DiscoveryConfidence.observedFact("OpenAPI metadata was explicitly parsed."), true, List.of(documentEvidence.evidenceId()),
                ContractDiscoverySupport.details("contractId", document.path(), "contractType", ContractType.API.name(),
                        "openApiVersion", version, "apiTitle", title, "contractVersion", apiVersion,
                        "explicitVersion", Boolean.toString(!apiVersion.isBlank())));
        evidence.add(metadata);
        observations.add(ContractDiscoverySupport.observation(ID, "openapi-contract-declared",
                "OpenAPI document " + document.path() + " declares version " + version + ".", metadata.confidence(), List.of(metadata)));

        for (Map.Entry<String, JsonNode> path : ContractDiscoverySupport.fields(root.path("paths"))) {
            for (Map.Entry<String, JsonNode> operation : ContractDiscoverySupport.fields(path.getValue())) {
                if (!METHODS.contains(operation.getKey().toLowerCase(Locale.ROOT))) continue;
                String method = operation.getKey().toUpperCase(Locale.ROOT);
                String operationId = ContractDiscoverySupport.text(operation.getValue(), "operationId");
                ContractEndpoint endpoint = new ContractEndpoint(ContractId.of(document.path()), path.getKey(), operationId, method);
                List<String> requestSchemas = ContractDiscoverySupport.schemaReferences(operation.getValue().path("requestBody"));
                List<String> responseSchemas = ContractDiscoverySupport.schemaReferences(operation.getValue().path("responses"));
                boolean deprecated = operation.getValue().path("deprecated").asBoolean(false);
                List<DiscoveryEvidence> spring = exactSpringEndpoint(input, endpoint);
                List<String> sourceIds = new ArrayList<>(List.of(metadata.evidenceId()));
                sourceIds.addAll(spring.stream().map(DiscoveryEvidence::evidenceId).toList());
                DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "api-operation-contract", input.rootDirectory(), document.absolutePath(),
                        method + " " + path.getKey(), ContractDiscoverySupport.line(document.content(), path.getKey()),
                        DiscoveryConfidence.observedFact("The API operation is explicitly declared in OpenAPI."), true, sourceIds,
                        ContractDiscoverySupport.details("contractId", endpoint.contractId().value(), "contractType", ContractType.API.name(),
                                "endpointPath", endpoint.path(), "httpMethod", endpoint.method(), "operationId", endpoint.operationId(),
                                "requestSchemas", String.join(",", requestSchemas), "responseSchemas", String.join(",", responseSchemas),
                                "deprecated", Boolean.toString(deprecated), "springCorrelation", Boolean.toString(!spring.isEmpty())));
                evidence.add(item);
                observations.add(ContractDiscoverySupport.observation(ID, "api-operation-declared",
                        method + " " + path.getKey() + " is declared by " + document.path() + ".", item.confidence(), List.of(item)));
                for (DiscoveryEvidence springEndpoint : spring) {
                    DiscoveryEvidence correlation = ContractDiscoverySupport.evidence(ID, "spring-openapi-endpoint-correlation",
                            input.rootDirectory(), document.absolutePath(), method + " " + path.getKey() + "->" + springEndpoint.identity(),
                            ContractDiscoverySupport.line(document.content(), path.getKey()),
                            DiscoveryConfidence.high("The OpenAPI and Spring endpoints have the same static HTTP method and path."), false,
                            List.of(item.evidenceId(), springEndpoint.evidenceId()),
                            ContractDiscoverySupport.details("contractId", document.path(), "endpointPath", endpoint.path(),
                                    "httpMethod", endpoint.method(), "openApiOperation", operationId,
                                    "springSymbol", springEndpoint.attributes().getOrDefault("symbol", springEndpoint.identity()),
                                    "uncertainty", "exact-method-and-path-correlation"));
                    evidence.add(correlation);
                    observations.add(ContractDiscoverySupport.observation(ID, "spring-openapi-endpoint-correlated",
                            method + " " + path.getKey() + " exactly matches Spring endpoint "
                                    + correlation.attributes().get("springSymbol") + ".", correlation.confidence(), List.of(correlation)));
                }
                for (String schema : requestSchemas) evidence.add(schemaUse(input, document, item, schema, "request"));
                for (String schema : responseSchemas) evidence.add(schemaUse(input, document, item, schema, "response"));
            }
        }
        for (Map.Entry<String, JsonNode> schema : ContractDiscoverySupport.fields(root.path("components").path("schemas"))) {
            evidence.add(ContractDiscoverySupport.evidence(ID, "api-component-schema", input.rootDirectory(), document.absolutePath(),
                    document.path() + "#/components/schemas/" + schema.getKey(), ContractDiscoverySupport.line(document.content(), schema.getKey()),
                    DiscoveryConfidence.observedFact("The component schema is explicitly declared."), true, List.of(metadata.evidenceId()),
                    ContractDiscoverySupport.details("contractId", document.path(), "contractType", ContractType.SCHEMA.name(), "schemaName", schema.getKey())));
        }
        for (Map.Entry<String, JsonNode> scheme : ContractDiscoverySupport.fields(root.path("components").path("securitySchemes"))) {
            evidence.add(ContractDiscoverySupport.evidence(ID, "api-security-scheme", input.rootDirectory(), document.absolutePath(),
                    scheme.getKey(), ContractDiscoverySupport.line(document.content(), scheme.getKey()),
                    DiscoveryConfidence.observedFact("The security scheme is explicitly declared."), true, List.of(metadata.evidenceId()),
                    ContractDiscoverySupport.details("contractId", document.path(), "schemeName", scheme.getKey(),
                            "schemeType", ContractDiscoverySupport.text(scheme.getValue(), "type"))));
        }
        int serverIndex = 0;
        for (JsonNode server : root.path("servers")) {
            String url = ContractDiscoverySupport.text(server, "url");
            boolean dynamic = ContractDiscoverySupport.dynamic(url);
            evidence.add(ContractDiscoverySupport.evidence(ID, "api-server", input.rootDirectory(), document.absolutePath(),
                    document.path() + ":server:" + serverIndex++, ContractDiscoverySupport.line(document.content(), url),
                    dynamic ? DiscoveryConfidence.inferred(0.7, "The server URL contains an unresolved expression.")
                            : DiscoveryConfidence.observedFact("The server URL is explicitly declared."), !dynamic, List.of(metadata.evidenceId()),
                    ContractDiscoverySupport.details("contractId", document.path(), "serverUrl", url,
                            "uncertainty", dynamic ? "dynamic-expression" : "")));
        }
    }

    private static DiscoveryEvidence schemaUse(DiscoveryInput input, ContractDiscoverySupport.Document document,
                                               DiscoveryEvidence operation, String schema, String direction) {
        return ContractDiscoverySupport.evidence(ID, "api-schema-reference", input.rootDirectory(), document.absolutePath(),
                operation.identity() + ":" + direction + ":" + schema, ContractDiscoverySupport.line(document.content(), schema),
                DiscoveryConfidence.observedFact("The operation explicitly references a schema."), true, List.of(operation.evidenceId()),
                ContractDiscoverySupport.details("contractId", document.path(), "schemaName", schema, "direction", direction,
                        "operation", operation.identity()));
    }

    private static List<DiscoveryEvidence> exactSpringEndpoint(DiscoveryInput input, ContractEndpoint endpoint) {
        return ContractDiscoverySupport.priorEvidence(input, "spring-http-endpoint").stream()
                .filter(item -> endpoint.path().equals(item.attributes().get("endpointPath")))
                .filter(item -> endpoint.method().equalsIgnoreCase(item.attributes().getOrDefault("httpMethod", ""))).toList();
    }

    private static String marker(String content, String... keys) {
        for (String key : keys) {
            var matcher = java.util.regex.Pattern.compile("(?m)^\\s*\\\"?" + key + "\\\"?\\s*:\\s*\\\"?([^\\\"\\s,}]+)").matcher(content);
            if (matcher.find()) return matcher.group(1);
        }
        return "";
    }

    private static String extension(String path) { return path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT); }
    private static String first(String first, String second) { return first.isBlank() ? second : first; }
}
