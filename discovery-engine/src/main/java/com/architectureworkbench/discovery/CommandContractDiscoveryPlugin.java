package com.architectureworkbench.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers explicit and convention-labelled command contracts and SQS message endpoints. */
public class CommandContractDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("contract.command");
    private static final Pattern QUEUE_VALUE = Pattern.compile("\\.(?:queue|queueUrl|queueName)\\s*\\(\\s*\"([^\"]+)\"");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Command Contract Discovery Plugin", "0.2.4", "Contract Plugin",
                List.of("java", "json-schema", "sqs"), List.of(DiscoveryPluginCapability.DETECT_COMMAND_CONTRACTS),
                List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(JavaStructureDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(SpringMessagingDiscoveryPlugin.ID, false)), true);
    }

    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        boolean partial = false;
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            if (!ContractDiscoverySupport.structured(file)) continue;
            var parsed = ContractDiscoverySupport.document(input.rootDirectory(), file);
            if (parsed.isEmpty() || !ContractDiscoverySupport.commandSchemaCandidate(parsed.get())) continue;
            var document = parsed.get();
            if (!document.valid()) {
                partial = true;
                DiscoveryEvidence error = ContractDiscoverySupport.parseError(ID, input.rootDirectory(), document, "command-schema");
                evidence.add(error);
                diagnostics.add("INVALID_CONTRACT_DOCUMENT|" + document.path() + "|" + document.parseError());
                continue;
            }
            inspectSchema(input, document, evidence, observations);
        }
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics))
            inspectJava(input, source, evidence, observations);
        return ContractDiscoverySupport.result(ID, started, evidence, observations, diagnostics, partial);
    }

    private static void inspectSchema(DiscoveryInput input, ContractDiscoverySupport.Document document,
                                      List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        JsonNode root = document.root();
        String name = first(ContractDiscoverySupport.text(root, "x-command-type"), ContractDiscoverySupport.text(root, "commandType"),
                ContractDiscoverySupport.text(root, "command-type"), ContractDiscoverySupport.text(root, "title"),
                ContractDiscoverySupport.fileStem(document.path()));
        String version = ContractDiscoverySupport.explicitVersion(root);
        String queue = first(ContractDiscoverySupport.text(root, "queue"), ContractDiscoverySupport.text(root, "queueName"),
                ContractDiscoverySupport.text(root, "x-queue"));
        String replyQueue = first(ContractDiscoverySupport.text(root, "replyQueue"), ContractDiscoverySupport.text(root, "responseQueue"));
        boolean correlation = has(root, "correlationId");
        boolean causation = has(root, "causationId");
        boolean envelope = has(root, "commandType") || has(root, "payload") || correlation || causation || !replyQueue.isBlank();
        String compatibility = first(ContractDiscoverySupport.text(root, "compatibility"), ContractDiscoverySupport.text(root, "x-compatibility"));
        boolean deprecated = root.path("deprecated").asBoolean(false);
        String supersededBy = first(ContractDiscoverySupport.text(root, "supersededBy"), ContractDiscoverySupport.text(root, "x-superseded-by"));
        DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "command-contract", input.rootDirectory(), document.absolutePath(), name,
                ContractDiscoverySupport.line(document.content(), name),
                DiscoveryConfidence.observedFact("The command schema explicitly declares a structured contract."), true, List.of(),
                ContractDiscoverySupport.details("contractId", name, "contractType", ContractType.COMMAND.name(), "commandName", name,
                        "contractVersion", version, "explicitVersion", Boolean.toString(!version.isBlank()), "schemaPath", document.path(),
                        "queueName", queue, "replyQueue", replyQueue, "envelope", Boolean.toString(envelope),
                        "correlationField", Boolean.toString(correlation), "causationField", Boolean.toString(causation),
                        "versionField", Boolean.toString(has(root.path("properties"), "version")), "compatibility", compatibility,
                        "deprecated", Boolean.toString(deprecated), "supersededBy", supersededBy));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "command-contract-declared",
                name + (version.isBlank() ? "" : " v" + version) + " is defined in " + document.path() + ".", item.confidence(), List.of(item)));
        if (envelope) evidence.add(ContractDiscoverySupport.evidence(ID, "command-envelope", input.rootDirectory(), document.absolutePath(),
                name + ":envelope", ContractDiscoverySupport.line(document.content(), "payload"),
                DiscoveryConfidence.observedFact("Command envelope fields are explicitly declared."), true, List.of(item.evidenceId()),
                ContractDiscoverySupport.details("contractId", name, "commandName", name, "queueName", queue,
                        "replyQueue", replyQueue, "correlationField", Boolean.toString(correlation), "causationField", Boolean.toString(causation))));
    }

    private static void inspectJava(DiscoveryInput input, SpringDiscoverySupport.Source source,
                                    List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        boolean explicit = source.annotations().stream().anyMatch(annotation ->
                List.of("Command", "CommandContract", "CommandType").contains(annotation.name()));
        boolean convention = source.className().endsWith("Command") || source.className().endsWith("CommandEnvelope")
                || ("record".equals(source.kind()) && source.path().toLowerCase(Locale.ROOT).contains("/command"));
        boolean sqsEnvelope = source.className().toLowerCase(Locale.ROOT).contains("sqs")
                && (source.content().contains("commandType") || source.content().contains("messageBody"));
        if (explicit || convention || sqsEnvelope) {
            String version = javaVersion(source);
            boolean envelope = source.className().contains("Envelope") || source.content().contains("correlationId")
                    || source.content().contains("causationId") || source.content().contains("replyQueue");
            DiscoveryConfidence confidence = explicit
                    ? DiscoveryConfidence.observedFact("An explicit command contract annotation is present.")
                    : DiscoveryConfidence.inferred(0.75, "The Java type follows a clear command or SQS envelope convention.");
            DiscoveryEvidence item = ContractDiscoverySupport.sourceEvidence(ID, "java-command-contract", source, source.qualifiedName(),
                    source.typeLine(), confidence, explicit, List.of(),
                    ContractDiscoverySupport.details("contractId", source.className(), "contractType", ContractType.COMMAND.name(),
                            "commandName", source.className(), "contractVersion", version, "explicitVersion", Boolean.toString(!version.isBlank()),
                            "javaType", source.kind(), "envelope", Boolean.toString(envelope),
                            "versionField", Boolean.toString(Pattern.compile("\\b(?:schema)?version\\b", Pattern.CASE_INSENSITIVE).matcher(source.content()).find()),
                            "correlationField", Boolean.toString(source.content().contains("correlationId")),
                            "causationField", Boolean.toString(source.content().contains("causationId")),
                            "replyQueue", source.content().contains("replyQueue") ? "declared" : "",
                            "classificationBasis", explicit ? "annotation" : "naming-or-location-convention",
                            "uncertainty", explicit ? "" : "convention-based-classification"));
            evidence.add(item);
            observations.add(ContractDiscoverySupport.observation(ID, "java-command-contract-discovered",
                    source.className() + " is classified as a command contract by " + (explicit ? "annotation" : "convention") + ".",
                    item.confidence(), List.of(item)));
        }
        inspectSqs(source, evidence, observations);
    }

    private static void inspectSqs(SpringDiscoverySupport.Source source, List<DiscoveryEvidence> evidence,
                                   List<DiscoveryObservation> observations) {
        for (SpringDiscoverySupport.Method method : source.methods()) {
            var listener = SpringDiscoverySupport.annotation(method.annotations(), "SqsListener");
            if (listener.isEmpty()) continue;
            String queue = SpringDiscoverySupport.firstString(listener.get().arguments());
            boolean dynamic = ContractDiscoverySupport.dynamic(queue);
            List<SpringDiscoverySupport.Parameter> parameters = SpringDiscoverySupport.parameters(method.parameters());
            String command = parameters.isEmpty() ? "" : SpringDiscoverySupport.simpleType(parameters.get(0).type());
            DiscoveryEvidence item = ContractDiscoverySupport.sourceEvidence(ID, "command-consumer-reference", source,
                    method.symbol(source) + ":" + queue, listener.get().line(),
                    dynamic ? DiscoveryConfidence.inferred(0.7, "The SQS queue contains an unresolved expression.")
                            : DiscoveryConfidence.observedFact("The SQS listener and queue are explicitly declared."), !dynamic, List.of(),
                    ContractDiscoverySupport.details("contractId", command.isBlank() ? queue : command, "commandName", command,
                            "consumer", source.qualifiedName() + "." + method.name(), "queueName", queue,
                            "channelName", queue, "channelType", MessageChannelType.QUEUE.name(),
                            "uncertainty", dynamic ? "dynamic-expression" : ""));
            evidence.add(item);
            observations.add(ContractDiscoverySupport.observation(ID, "command-consumer-declared",
                    method.symbol(source) + " consumes " + (command.isBlank() ? "commands" : command) + " from " + queue + ".",
                    item.confidence(), List.of(item)));
        }
        if (!(source.content().contains("SqsClient") || source.content().contains("SqsTemplate") || source.content().contains("sendMessage("))) return;
        Matcher matcher = QUEUE_VALUE.matcher(source.content());
        boolean found = false;
        while (matcher.find()) {
            found = true;
            addProducer(source, matcher.group(1), ContractDiscoverySupport.line(source.content(), matcher.group()), evidence, observations);
        }
        if (!found && source.content().contains("sendMessage(")) addProducer(source, "", ContractDiscoverySupport.line(source.content(), "sendMessage("), evidence, observations);
    }

    private static void addProducer(SpringDiscoverySupport.Source source, String queue, int line,
                                    List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        boolean dynamic = queue.isBlank() || ContractDiscoverySupport.dynamic(queue);
        String command = commandReference(source.content());
        DiscoveryEvidence item = ContractDiscoverySupport.sourceEvidence(ID, "command-producer-reference", source,
                source.qualifiedName() + ":sendMessage:" + queue, line,
                dynamic ? DiscoveryConfidence.inferred(0.65, "SQS publishing is present but the queue is unresolved.")
                        : DiscoveryConfidence.observedFact("The SQS producer and static queue are explicitly present."), !dynamic, List.of(),
                ContractDiscoverySupport.details("contractId", command.isBlank() ? queue : command, "commandName", command,
                        "producer", source.qualifiedName(), "queueName", queue, "channelName", queue,
                        "channelType", MessageChannelType.QUEUE.name(), "publishOperation", "sendMessage",
                        "uncertainty", dynamic ? "unresolved-queue" : ""));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "command-producer-declared",
                source.className() + " publishes " + (command.isBlank() ? "a command" : command) + (queue.isBlank() ? " to SQS." : " to " + queue + "."),
                item.confidence(), List.of(item)));
    }

    private static String commandReference(String content) {
        Matcher matcher = Pattern.compile("new\\s+([A-Za-z_$][\\w$]*Command)\\s*\\(").matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String javaVersion(SpringDiscoverySupport.Source source) {
        Matcher matcher = Pattern.compile("(?i)(?:schema)?version\\s*[=(]\\s*\"?([0-9]+(?:\\.[0-9]+){0,2})").matcher(source.content());
        return matcher.find() ? matcher.group(1) : ContractDiscoverySupport.pathVersion(source.path());
    }

    private static boolean has(JsonNode node, String name) {
        if (node == null) return false;
        if (node.isObject()) {
            if (node.has(name)) return true;
            var iterator = node.elements();
            while (iterator.hasNext()) if (has(iterator.next(), name)) return true;
        } else if (node.isArray()) for (JsonNode child : node) if (has(child, name)) return true;
        return false;
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
