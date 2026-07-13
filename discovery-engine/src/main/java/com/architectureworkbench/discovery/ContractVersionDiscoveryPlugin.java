package com.architectureworkbench.discovery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Collects explicit contract versioning and factual absence observations. */
public class ContractVersionDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("contract.version");
    private static final Pattern VERSION_HEADER = Pattern.compile("(?i)(X-(?:API|Schema|Event|Command)-Version|Accept-Version|api-version)(?:[\"']?\\s*[:=,]\\s*[\"']?([A-Za-z0-9._${}#-]+))?");
    private static final List<String> CONTRACT_TYPES = List.of("api-contract", "openapi-document", "asyncapi-document", "event-contract", "java-event-contract",
            "command-contract", "java-command-contract");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Contract Version Discovery Plugin", "0.2.4", "Contract Plugin",
                List.of("openapi", "asyncapi", "json-schema", "avro", "java"),
                List.of(DiscoveryPluginCapability.DETECT_CONTRACT_VERSIONS),
                List.of(new DiscoveryPluginDependency(OpenApiContractDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(EventContractDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(CommandContractDiscoveryPlugin.ID, false)), true);
    }

    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        Map<String, DiscoveryEvidence> contracts = new LinkedHashMap<>();
        for (DiscoveryEvidence item : ContractDiscoverySupport.priorEvidence(input)) {
            if (!CONTRACT_TYPES.contains(item.evidenceType())) continue;
            String contractId = item.attributes().getOrDefault("contractId", item.identity());
            String key = contractId + "|" + item.attributes().getOrDefault("filePath", "");
            contracts.merge(key, item, ContractVersionDiscoveryPlugin::preferVersioned);
        }
        for (DiscoveryEvidence contract : contracts.values())
            inspectContract(input, contract.attributes().getOrDefault("contractId", contract.identity()), contract, evidence, observations);
        inspectFiles(input, evidence, observations, diagnostics);
        return ContractDiscoverySupport.result(ID, started, evidence, observations, diagnostics, false);
    }

    private static void inspectContract(DiscoveryInput input, String contractId, DiscoveryEvidence source,
                                        List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        String version = first(source.attributes().get("contractVersion"), source.attributes().get("apiVersion"));
        String declaration = "declared-field";
        if (version.isBlank()) {
            version = ContractDiscoverySupport.pathVersion(source.attributes().getOrDefault("filePath", ""));
            declaration = "versioned-path";
        }
        String formatVersion = first(source.attributes().get("openApiVersion"), source.attributes().get("asyncApiVersion"));
        if (!formatVersion.isBlank()) addFormatVersion(input, contractId, source, formatVersion, evidence, observations);
        addCompatibility(input, contractId, source, evidence);
        if (version.isBlank()) {
            observations.add(ContractDiscoverySupport.observation(ID, "contract-version-absent",
                    label(source, contractId) + " contains no explicit version declaration.",
                    DiscoveryConfidence.observedFact("The discovered contract evidence exposes no explicit version field or versioned path."), List.of(source)));
            return;
        }
        ContractVersion contractVersion = new ContractVersion(version, true, declaration);
        Path file = input.rootDirectory().resolve(source.attributes().getOrDefault("filePath", source.references().get(0)));
        DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "contract-version", input.rootDirectory(), file,
                contractId + ":" + version, parseLine(source.attributes().get("line")),
                DiscoveryConfidence.observedFact("The contract version is explicitly declared or encoded in its path."), true,
                List.of(source.evidenceId()), ContractDiscoverySupport.details("contractId", contractId,
                        "contractVersion", contractVersion.value(), "versionSource", contractVersion.source(), "explicitVersion", "true",
                        "semanticVersion", Boolean.toString(!ContractDiscoverySupport.semanticVersion(contractVersion.value()).isBlank())));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "contract-version-declared",
                label(source, contractId) + " has explicit version " + version + ".", item.confidence(), List.of(item)));
    }

    private static void inspectFiles(DiscoveryInput input, List<DiscoveryEvidence> evidence,
                                     List<DiscoveryObservation> observations, List<String> diagnostics) {
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".java") || lower.endsWith(".json") || lower.endsWith(".yaml") || lower.endsWith(".yml")
                    || lower.endsWith(".properties") || lower.endsWith(".md"))) continue;
            try {
                String content = Files.readString(file);
                Matcher matcher = VERSION_HEADER.matcher(content);
                while (matcher.find()) {
                    String value = matcher.group(2) == null ? "" : matcher.group(2);
                    boolean dynamic = ContractDiscoverySupport.dynamic(value);
                    DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "contract-version-header", input.rootDirectory(), file,
                            matcher.group(1) + ":" + value, ContractDiscoverySupport.line(content, matcher.group()),
                            dynamic ? DiscoveryConfidence.inferred(0.7, "The version header value is dynamic or unresolved.")
                                    : DiscoveryConfidence.observedFact("A contract version header is explicitly declared."), !dynamic, List.of(),
                            ContractDiscoverySupport.details("versionHeader", matcher.group(1), "contractVersion", value,
                                    "uncertainty", dynamic ? "dynamic-expression" : ""));
                    evidence.add(item);
                    observations.add(ContractDiscoverySupport.observation(ID, "contract-version-header-declared",
                            matcher.group(1) + " is declared" + (value.isBlank() ? "." : " with value " + value + "."), item.confidence(), List.of(item)));
                }
            } catch (Exception exception) {
                diagnostics.add("Unable to inspect version declarations in " + ContractDiscoverySupport.relative(input.rootDirectory(), file));
            }
        }
    }

    private static String label(DiscoveryEvidence source, String contractId) {
        String type = source.attributes().getOrDefault("contractType", "contract").toLowerCase(Locale.ROOT);
        return type + " contract " + contractId;
    }

    private static void addFormatVersion(DiscoveryInput input, String contractId, DiscoveryEvidence source, String formatVersion,
                                         List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        Path formatFile = input.rootDirectory().resolve(source.attributes().getOrDefault("filePath", source.references().get(0)));
        boolean openApi = source.attributes().containsKey("openApiVersion");
        DiscoveryEvidence format = ContractDiscoverySupport.evidence(ID, "contract-format-version", input.rootDirectory(), formatFile,
                contractId + ":format:" + formatVersion, parseLine(source.attributes().get("line")),
                DiscoveryConfidence.observedFact("The contract document format version is explicitly declared."), true,
                List.of(source.evidenceId()), ContractDiscoverySupport.details("contractId", contractId, "formatVersion", formatVersion,
                        "format", openApi ? "openapi" : "asyncapi"));
        evidence.add(format);
        observations.add(ContractDiscoverySupport.observation(ID, "contract-format-version-declared",
                (openApi ? "OpenAPI" : "AsyncAPI") + " document declares version " + formatVersion + ".",
                format.confidence(), List.of(format)));
    }

    private static void addCompatibility(DiscoveryInput input, String contractId, DiscoveryEvidence source,
                                         List<DiscoveryEvidence> evidence) {
        String compatibility = source.attributes().getOrDefault("compatibility", "");
        boolean deprecated = Boolean.parseBoolean(source.attributes().getOrDefault("deprecated", "false"));
        String supersededBy = source.attributes().getOrDefault("supersededBy", "");
        if (compatibility.isBlank() && !deprecated && supersededBy.isBlank()) return;
        Path file = input.rootDirectory().resolve(source.attributes().getOrDefault("filePath", source.references().get(0)));
        ContractCompatibilityEvidence typed = new ContractCompatibilityEvidence(ContractId.of(contractId), compatibility,
                supersededBy, deprecated, DiscoveryConfidence.observedFact("Compatibility metadata is explicit."));
        evidence.add(ContractDiscoverySupport.evidence(ID, "contract-compatibility", input.rootDirectory(), file,
                contractId + ":compatibility", parseLine(source.attributes().get("line")), typed.confidence(), true,
                List.of(source.evidenceId()), ContractDiscoverySupport.details("contractId", typed.contractId().value(),
                        "compatibility", typed.mode(), "deprecated", Boolean.toString(typed.deprecated()),
                        "supersededBy", typed.supersedes())));
    }

    private static DiscoveryEvidence preferVersioned(DiscoveryEvidence left, DiscoveryEvidence right) {
        boolean leftVersioned = !first(left.attributes().get("contractVersion"), left.attributes().get("apiVersion")).isBlank();
        boolean rightVersioned = !first(right.attributes().get("contractVersion"), right.attributes().get("apiVersion")).isBlank();
        return rightVersioned && !leftVersioned ? right : left;
    }

    private static int parseLine(String value) { try { return Integer.parseInt(value); } catch (Exception ignored) { return 1; } }
    private static String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return ""; }
}
