package com.architectureworkbench.discovery;

import com.fasterxml.jackson.databind.JsonNode;
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

/** Collects independent, confidence-labelled ownership indicators without choosing a canonical owner. */
public class ContractOwnershipEvidencePlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("contract.ownership");
    private static final Pattern DOC_OWNER = Pattern.compile("(?im)^\\s*(?:owner|team|maintainer|contact)\\s*[:=]\\s*([^#\\r\\n]+)");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Contract Ownership Evidence Plugin", "0.2.4", "Contract Plugin",
                List.of("codeowners", "maven", "openapi", "asyncapi", "json-schema", "documentation"),
                List.of(DiscoveryPluginCapability.DETECT_CONTRACT_OWNERSHIP),
                List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(MavenDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(OpenApiContractDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(EventContractDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(CommandContractDiscoveryPlugin.ID, false)), true);
    }

    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        List<DiscoveryEvidence> contracts = ContractDiscoverySupport.priorEvidence(input).stream()
                .filter(item -> item.attributes().containsKey("contractId")).toList();
        codeOwners(input, contracts, evidence, observations, diagnostics);
        structuredMetadata(input, evidence, observations, diagnostics);
        mavenAndNamespace(input, contracts, evidence, observations);
        documentation(input, evidence, observations, diagnostics);
        return ContractDiscoverySupport.result(ID, started, evidence, observations, diagnostics, false);
    }

    private static void codeOwners(DiscoveryInput input, List<DiscoveryEvidence> contracts, List<DiscoveryEvidence> evidence,
                                   List<DiscoveryObservation> observations, List<String> diagnostics) {
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            if (!file.getFileName().toString().equals("CODEOWNERS")) continue;
            try {
                List<String> lines = Files.readAllLines(file);
                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index).trim();
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) continue;
                    String pattern = parts[0];
                    String owners = String.join(",", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                    List<DiscoveryEvidence> matched = contracts.stream().filter(contract ->
                            matches(pattern, contract.attributes().getOrDefault("filePath", ""))).toList();
                    if (matched.isEmpty()) {
                        addOwnership(input, file, index + 1, "repository-contracts", owners, "CODEOWNERS", true,
                                DiscoveryConfidence.observedFact("A repository ownership rule is explicitly declared."), List.of(), pattern, evidence, observations);
                    } else for (DiscoveryEvidence contract : matched) {
                        boolean exact = !pattern.contains("*");
                        addOwnership(input, file, index + 1, contract.attributes().get("contractId"), owners, "CODEOWNERS", exact,
                                exact ? DiscoveryConfidence.observedFact("CODEOWNERS explicitly names this contract path.")
                                        : DiscoveryConfidence.high("The contract path deterministically matches a CODEOWNERS pattern."),
                                List.of(contract.evidenceId()), pattern, evidence, observations);
                    }
                }
            } catch (Exception exception) {
                diagnostics.add("Unable to inspect CODEOWNERS: " + exception.getMessage());
            }
        }
    }

    private static void structuredMetadata(DiscoveryInput input, List<DiscoveryEvidence> evidence,
                                           List<DiscoveryObservation> observations, List<String> diagnostics) {
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            if (!ContractDiscoverySupport.structured(file)) continue;
            var parsed = ContractDiscoverySupport.document(input.rootDirectory(), file);
            if (parsed.isEmpty() || !parsed.get().valid()) continue;
            JsonNode root = parsed.get().root();
            String owner = first(ContractDiscoverySupport.text(root, "owner"), ContractDiscoverySupport.text(root, "x-owner"),
                    ContractDiscoverySupport.text(root, "info", "contact", "name"), ContractDiscoverySupport.text(root, "info", "contact", "email"));
            if (owner.isBlank()) continue;
            String contractId = first(ContractDiscoverySupport.text(root, "x-event-type"), ContractDiscoverySupport.text(root, "eventType"),
                    ContractDiscoverySupport.text(root, "x-command-type"), ContractDiscoverySupport.text(root, "commandType"),
                    ContractDiscoverySupport.text(root, "info", "title"), ContractDiscoverySupport.text(root, "title"), parsed.get().path());
            addOwnership(input, file, ContractDiscoverySupport.line(parsed.get().content(), owner), contractId, owner,
                    root.path("info").path("contact").isMissingNode() ? "schema-metadata" : "contract-contact", true,
                    DiscoveryConfidence.observedFact("Ownership or contact metadata is explicitly declared in the contract."), List.of(), "", evidence, observations);
        }
    }

    private static void mavenAndNamespace(DiscoveryInput input, List<DiscoveryEvidence> contracts, List<DiscoveryEvidence> evidence,
                                          List<DiscoveryObservation> observations) {
        List<DiscoveryEvidence> modules = ContractDiscoverySupport.priorEvidence(input, "build-module");
        for (DiscoveryEvidence contract : contracts) {
            String contractModule = contract.attributes().getOrDefault("module", ".");
            modules.stream().filter(module -> contractModule.equals(module.attributes().getOrDefault("module", "."))).forEach(module -> {
                String coordinate = first(module.attributes().get("groupId"), module.attributes().get("artifactId"));
                if (coordinate.isBlank()) return;
                Path file = input.rootDirectory().resolve(contract.attributes().getOrDefault("filePath", contract.references().get(0)));
                addOwnership(input, file, parseLine(contract.attributes().get("line")), contract.attributes().get("contractId"), coordinate,
                        "maven-coordinate", false, DiscoveryConfidence.inferred(0.65, "Maven namespace is an ownership indicator, not an owner declaration."),
                        List.of(contract.evidenceId(), module.evidenceId()), "", evidence, observations);
            });
            String packageName = contract.attributes().getOrDefault("packageName", "");
            if (!packageName.isBlank()) {
                Path file = input.rootDirectory().resolve(contract.attributes().getOrDefault("filePath", contract.references().get(0)));
                addOwnership(input, file, parseLine(contract.attributes().get("line")), contract.attributes().get("contractId"), packageName,
                        "package-namespace", false, DiscoveryConfidence.inferred(0.6, "A package namespace is a weak ownership indicator."),
                        List.of(contract.evidenceId()), "", evidence, observations);
            }
        }
    }

    private static void documentation(DiscoveryInput input, List<DiscoveryEvidence> evidence,
                                      List<DiscoveryObservation> observations, List<String> diagnostics) {
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".md") || lower.endsWith(".adoc") || lower.endsWith(".txt"))) continue;
            try {
                String content = Files.readString(file);
                Matcher matcher = DOC_OWNER.matcher(content);
                while (matcher.find()) addOwnership(input, file, ContractDiscoverySupport.line(content, matcher.group()),
                        ContractDiscoverySupport.relative(input.rootDirectory(), file), matcher.group(1).trim(), "documentation", false,
                        DiscoveryConfidence.inferred(0.75, "Documentation explicitly states an owner, but the contract scope may be broader."),
                        List.of(), "", evidence, observations);
            } catch (Exception exception) {
                diagnostics.add("Unable to inspect ownership documentation " + ContractDiscoverySupport.relative(input.rootDirectory(), file));
            }
        }
    }

    private static void addOwnership(DiscoveryInput input, Path file, int line, String contractId, String owner,
                                     String sourceKind, boolean direct, DiscoveryConfidence confidence,
                                     List<String> sourceIds, String pattern, List<DiscoveryEvidence> evidence,
                                     List<DiscoveryObservation> observations) {
        ContractOwnershipEvidence typed = new ContractOwnershipEvidence(ContractId.of(contractId), owner, sourceKind, direct, confidence);
        DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "contract-ownership-evidence", input.rootDirectory(), file,
                typed.contractId().value() + ":" + typed.sourceKind() + ":" + typed.owner(), line, typed.confidence(), direct, sourceIds,
                ContractDiscoverySupport.details("contractId", typed.contractId().value(), "owner", typed.owner(),
                        "ownershipSource", typed.sourceKind(), "directOwnershipDeclaration", Boolean.toString(typed.directlyDeclared()),
                        "codeownersPattern", pattern, "uncertainty", direct ? "" : "ownership-indicator-only"));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "contract-ownership-indicator-observed",
                typed.sourceKind() + " identifies " + typed.owner() + " for " + typed.contractId().value() + ".", item.confidence(), List.of(item)));
    }

    private static boolean matches(String pattern, String path) {
        String normalized = pattern.startsWith("/") ? pattern.substring(1) : pattern;
        StringBuilder regex = new StringBuilder();
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '*') {
                if (index + 1 < normalized.length() && normalized.charAt(index + 1) == '*') { regex.append(".*"); index++; }
                else regex.append("[^/]*");
            } else if (current == '?') regex.append("[^/]");
            else regex.append(Pattern.quote(String.valueOf(current)));
        }
        if (normalized.endsWith("/")) regex.append(".*");
        return path.matches(regex.toString()) || (!pattern.startsWith("/") && path.matches("(?:.*/)?" + regex));
    }

    private static int parseLine(String value) { try { return Integer.parseInt(value); } catch (Exception ignored) { return 1; } }
    private static String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return ""; }
}
