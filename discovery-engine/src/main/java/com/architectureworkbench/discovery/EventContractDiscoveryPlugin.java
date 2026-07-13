package com.architectureworkbench.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers explicit and convention-labelled event contracts and EventBridge declarations. */
public class EventContractDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("contract.event");
    private static final Set<String> EVENT_ANNOTATIONS = Set.of("DomainEvent", "EventContract", "EventType");
    private static final Pattern BUILDER_VALUE = Pattern.compile("\\.(detailType|eventBusName|source)\\s*\\(\\s*\"([^\"]+)\"");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Event Contract Discovery Plugin", "0.2.4", "Contract Plugin",
                List.of("json-schema", "avro", "asyncapi", "java", "eventbridge"),
                List.of(DiscoveryPluginCapability.DETECT_EVENT_CONTRACTS),
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
            if (parsed.isEmpty()) continue;
            var document = parsed.get();
            boolean async = ContractDiscoverySupport.asyncApiCandidate(document);
            if (!async && !ContractDiscoverySupport.eventSchemaCandidate(document)) continue;
            if (!document.valid()) {
                partial = true;
                DiscoveryEvidence error = ContractDiscoverySupport.parseError(ID, input.rootDirectory(), document, async ? "asyncapi" : "event-schema");
                evidence.add(error);
                diagnostics.add("INVALID_CONTRACT_DOCUMENT|" + document.path() + "|" + document.parseError());
                continue;
            }
            if (async) inspectAsyncApi(input, document, evidence, observations);
            else inspectSchema(input, document, evidence, observations);
        }
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics)) {
            inspectJava(input, source, evidence, observations);
            inspectEventBridge(input, source, evidence, observations);
            inspectApplicationPublisher(input, source, evidence, observations);
        }
        adaptSpringConsumers(input, evidence, observations);
        inspectEventBridgeConfiguration(input, evidence, observations, diagnostics);
        return ContractDiscoverySupport.result(ID, started, evidence, observations, diagnostics, partial);
    }

    private static void inspectSchema(DiscoveryInput input, ContractDiscoverySupport.Document document,
                                      List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        JsonNode root = document.root();
        String name = first(ContractDiscoverySupport.text(root, "x-event-type"), ContractDiscoverySupport.text(root, "eventType"),
                ContractDiscoverySupport.text(root, "detail-type"), ContractDiscoverySupport.text(root, "name"),
                ContractDiscoverySupport.text(root, "title"), ContractDiscoverySupport.fileStem(document.path()));
        String version = ContractDiscoverySupport.explicitVersion(root);
        boolean avro = document.path().toLowerCase(Locale.ROOT).endsWith(".avsc") || "record".equals(ContractDiscoverySupport.text(root, "type"));
        boolean envelope = has(root, "eventType") || has(root, "detail-type") || has(root, "detail") || has(root, "payload")
                || ContractDiscoverySupport.text(root, "title").toLowerCase(Locale.ROOT).contains("envelope");
        boolean versionField = has(root.path("properties"), "version") || avroField(root, "version");
        String compatibility = first(ContractDiscoverySupport.text(root, "compatibility"), ContractDiscoverySupport.text(root, "x-compatibility"));
        String owner = first(ContractDiscoverySupport.text(root, "owner"), ContractDiscoverySupport.text(root, "x-owner"));
        boolean deprecated = root.path("deprecated").asBoolean(false);
        String supersededBy = first(ContractDiscoverySupport.text(root, "supersededBy"), ContractDiscoverySupport.text(root, "x-superseded-by"));
        DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "event-contract", input.rootDirectory(), document.absolutePath(),
                name, ContractDiscoverySupport.line(document.content(), name),
                DiscoveryConfidence.observedFact("The event schema explicitly declares a structured contract."), true, List.of(),
                ContractDiscoverySupport.details("contractId", name, "contractType", ContractType.EVENT.name(), "eventName", name,
                        "contractVersion", version, "explicitVersion", Boolean.toString(!version.isBlank()), "schemaFormat", avro ? "avro" : "json-schema",
                        "schemaPath", document.path(), "envelope", Boolean.toString(envelope), "versionField", Boolean.toString(versionField),
                        "compatibility", compatibility, "deprecated", Boolean.toString(deprecated), "supersededBy", supersededBy,
                        "declaredOwner", owner));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "event-contract-declared",
                name + (version.isBlank() ? "" : " v" + version) + " is defined in " + document.path() + ".", item.confidence(), List.of(item)));
        if (envelope) evidence.add(ContractDiscoverySupport.evidence(ID, "event-envelope", input.rootDirectory(), document.absolutePath(),
                name + ":envelope", ContractDiscoverySupport.line(document.content(), "payload"),
                DiscoveryConfidence.observedFact("Envelope fields are explicitly present in the schema."), true, List.of(item.evidenceId()),
                ContractDiscoverySupport.details("contractId", name, "eventName", name, "envelope", "true")));
        if (!compatibility.isBlank()) evidence.add(compatibility(input, document, item, compatibility));
    }

    private static void inspectAsyncApi(DiscoveryInput input, ContractDiscoverySupport.Document document,
                                        List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        JsonNode root = document.root();
        String asyncVersion = ContractDiscoverySupport.text(root, "asyncapi");
        DiscoveryEvidence api = ContractDiscoverySupport.evidence(ID, "asyncapi-document", input.rootDirectory(), document.absolutePath(),
                document.path(), ContractDiscoverySupport.line(document.content(), "asyncapi"),
                DiscoveryConfidence.observedFact("The AsyncAPI version is explicitly declared."), true, List.of(),
                ContractDiscoverySupport.details("contractId", document.path(), "contractType", ContractType.EVENT.name(),
                        "asyncApiVersion", asyncVersion, "contractVersion", ContractDiscoverySupport.text(root, "info", "version"),
                        "contact", first(ContractDiscoverySupport.text(root, "info", "contact", "name"), ContractDiscoverySupport.text(root, "info", "contact", "email"))));
        evidence.add(api);
        for (Map.Entry<String, JsonNode> channelEntry : ContractDiscoverySupport.fields(root.path("channels"))) {
            String channelName = channelEntry.getKey();
            MessageChannel channel = new MessageChannel(channelName, channelType(channelName), ContractDiscoverySupport.dynamic(channelName));
            DiscoveryEvidence channelEvidence = ContractDiscoverySupport.evidence(ID, "message-channel", input.rootDirectory(), document.absolutePath(),
                    channel.name(), ContractDiscoverySupport.line(document.content(), channel.name()),
                    channel.dynamic() ? DiscoveryConfidence.inferred(0.7, "The channel contains an unresolved expression.")
                            : DiscoveryConfidence.observedFact("The AsyncAPI channel is explicitly declared."), !channel.dynamic(), List.of(api.evidenceId()),
                    ContractDiscoverySupport.details("channelName", channel.name(), "channelType", channel.type().name(),
                            "uncertainty", channel.dynamic() ? "dynamic-expression" : ""));
            evidence.add(channelEvidence);
            for (String operation : List.of("publish", "subscribe")) {
                JsonNode operationNode = channelEntry.getValue().path(operation);
                if (operationNode.isMissingNode()) continue;
                String messageName = first(ContractDiscoverySupport.text(operationNode, "message", "name"),
                        ContractDiscoverySupport.text(operationNode, "message", "$ref").replaceFirst("^.*/", ""), channelName);
                String role = operation.equals("publish") ? "producer" : "consumer";
                DiscoveryEvidence relation = ContractDiscoverySupport.evidence(ID, "asyncapi-" + role, input.rootDirectory(), document.absolutePath(),
                        role + ":" + channelName + ":" + messageName, ContractDiscoverySupport.line(document.content(), operation),
                        DiscoveryConfidence.observedFact("The AsyncAPI operation explicitly declares a messaging role."), true,
                        List.of(api.evidenceId(), channelEvidence.evidenceId()),
                        ContractDiscoverySupport.details("contractId", messageName, "eventName", messageName, "channelName", channelName,
                                "channelType", channel.type().name(), "role", role, "operationId", ContractDiscoverySupport.text(operationNode, "operationId")));
                evidence.add(relation);
                observations.add(ContractDiscoverySupport.observation(ID, "asyncapi-role-declared",
                        messageName + " has an explicit " + role + " on " + channelName + ".", relation.confidence(), List.of(relation)));
            }
        }
        for (Map.Entry<String, JsonNode> message : ContractDiscoverySupport.fields(root.path("components").path("messages"))) {
            String version = first(ContractDiscoverySupport.text(message.getValue(), "headers", "properties", "version", "const"),
                    ContractDiscoverySupport.text(message.getValue(), "x-version"));
            evidence.add(ContractDiscoverySupport.evidence(ID, "event-contract", input.rootDirectory(), document.absolutePath(), message.getKey(),
                    ContractDiscoverySupport.line(document.content(), message.getKey()),
                    DiscoveryConfidence.observedFact("The AsyncAPI message is explicitly declared."), true, List.of(api.evidenceId()),
                    ContractDiscoverySupport.details("contractId", message.getKey(), "contractType", ContractType.EVENT.name(),
                            "eventName", message.getKey(), "contractVersion", version, "explicitVersion", Boolean.toString(!version.isBlank()),
                            "schemaFormat", "asyncapi", "schemaPath", document.path())));
        }
    }

    private static void inspectJava(DiscoveryInput input, SpringDiscoverySupport.Source source,
                                    List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        boolean explicit = source.annotations().stream().anyMatch(annotation -> EVENT_ANNOTATIONS.contains(annotation.name()));
        boolean convention = source.className().endsWith("Event") || source.className().endsWith("EventEnvelope")
                || ("record".equals(source.kind()) && source.path().toLowerCase(Locale.ROOT).contains("/event"));
        if (!explicit && !convention) return;
        boolean envelope = source.className().contains("Envelope") || source.header().matches("(?s).*(eventType|detailType|payload|detail).*" );
        String version = versionFromJava(source);
        DiscoveryConfidence confidence = explicit
                ? DiscoveryConfidence.observedFact("An explicit event contract annotation is present.")
                : DiscoveryConfidence.inferred(0.75, "The Java type follows the event naming or location convention.");
        DiscoveryEvidence item = ContractDiscoverySupport.sourceEvidence(ID, "java-event-contract", source, source.qualifiedName(),
                source.typeLine(), confidence, explicit, List.of(),
                ContractDiscoverySupport.details("contractId", source.className(), "contractType", ContractType.EVENT.name(),
                        "eventName", source.className(), "contractVersion", version, "explicitVersion", Boolean.toString(!version.isBlank()),
                        "javaType", source.kind(), "envelope", Boolean.toString(envelope),
                        "versionField", Boolean.toString(Pattern.compile("\\b(?:schema)?version\\b", Pattern.CASE_INSENSITIVE).matcher(source.content()).find()),
                        "classificationBasis", explicit ? "annotation" : "naming-or-location-convention",
                        "uncertainty", explicit ? "" : "convention-based-classification"));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "java-event-contract-discovered",
                source.className() + " is classified as an event contract by " + (explicit ? "annotation" : "convention") + ".",
                item.confidence(), List.of(item)));
    }

    private static void inspectEventBridge(DiscoveryInput input, SpringDiscoverySupport.Source source,
                                           List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        if (!source.content().contains("putEvents(")) return;
        Map<String, String> values = new java.util.LinkedHashMap<>();
        Matcher matcher = BUILDER_VALUE.matcher(source.content());
        while (matcher.find()) values.putIfAbsent(matcher.group(1), matcher.group(2));
        String detailType = values.getOrDefault("detailType", "");
        String bus = values.getOrDefault("eventBusName", "");
        String eventName = detailType.isBlank() ? "EventBridgeEvent" : detailType;
        DiscoveryEvidence item = ContractDiscoverySupport.sourceEvidence(ID, "event-producer-reference", source,
                source.qualifiedName() + ":putEvents:" + eventName, ContractDiscoverySupport.line(source.content(), "putEvents("),
                DiscoveryConfidence.observedFact("EventBridge putEvents and its static values are directly present."), true, List.of(),
                ContractDiscoverySupport.details("contractId", eventName, "eventName", eventName, "producer", source.qualifiedName(),
                        "channelName", bus, "channelType", MessageChannelType.EVENT_BUS.name(), "detailType", detailType,
                        "source", values.getOrDefault("source", ""), "publishOperation", "putEvents"));
        evidence.add(item);
        observations.add(ContractDiscoverySupport.observation(ID, "eventbridge-producer-declared",
                source.className() + " publishes " + eventName + (bus.isBlank() ? "" : " to EventBridge bus " + bus) + ".",
                item.confidence(), List.of(item)));
    }

    private static void inspectApplicationPublisher(DiscoveryInput input, SpringDiscoverySupport.Source source,
                                                    List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        if (!source.content().contains("publishEvent(")) return;
        Matcher matcher = Pattern.compile("publishEvent\\s*\\(\\s*new\\s+([A-Za-z_$][\\w$]*(?:Event|ed))\\s*\\(").matcher(source.content());
        while (matcher.find()) {
            String eventName = matcher.group(1);
            DiscoveryEvidence item = ContractDiscoverySupport.sourceEvidence(ID, "event-producer-reference", source,
                    source.qualifiedName() + ":publishEvent:" + eventName, ContractDiscoverySupport.line(source.content(), matcher.group()),
                    DiscoveryConfidence.observedFact("The source directly publishes an exact event type."), true, List.of(),
                    ContractDiscoverySupport.details("contractId", eventName, "eventName", eventName,
                            "producer", source.qualifiedName(), "publishOperation", "publishEvent",
                            "channelName", "application-event-publisher", "channelType", MessageChannelType.UNKNOWN.name()));
            evidence.add(item);
            observations.add(ContractDiscoverySupport.observation(ID, "event-producer-declared",
                    source.className() + " publishes " + eventName + ".", item.confidence(), List.of(item)));
        }
    }

    private static void adaptSpringConsumers(DiscoveryInput input, List<DiscoveryEvidence> evidence,
                                             List<DiscoveryObservation> observations) {
        for (DiscoveryEvidence spring : ContractDiscoverySupport.priorEvidence(input, "spring-message-listener")) {
            String eventName = spring.attributes().getOrDefault("eventType", "");
            if (eventName.isBlank()) continue;
            String consumer = spring.attributes().getOrDefault("symbol", spring.identity());
            String channel = spring.attributes().getOrDefault("destination", "");
            Path file = input.rootDirectory().resolve(spring.attributes().getOrDefault("filePath", spring.references().get(0)));
            boolean dynamic = ContractDiscoverySupport.dynamic(channel);
            DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "event-consumer-reference", input.rootDirectory(), file,
                    eventName + "->" + consumer, parseLine(spring.attributes().get("line")),
                    dynamic ? DiscoveryConfidence.inferred(0.7, "The consumer is explicit but its channel is dynamic.")
                            : DiscoveryConfidence.observedFact("The listener explicitly declares its event type and consumer."), !dynamic,
                    List.of(spring.evidenceId()), ContractDiscoverySupport.details("contractId", eventName, "eventName", eventName,
                            "consumer", consumer, "channelName", channel, "channelType", listenerChannel(spring),
                            "uncertainty", dynamic ? "dynamic-expression" : ""));
            evidence.add(item);
            observations.add(ContractDiscoverySupport.observation(ID, "event-consumer-declared",
                    consumer + " consumes " + eventName + ".", item.confidence(), List.of(item)));
        }
    }

    private static void inspectEventBridgeConfiguration(DiscoveryInput input, List<DiscoveryEvidence> evidence,
                                                        List<DiscoveryObservation> observations, List<String> diagnostics) {
        Pattern resource = Pattern.compile("(?m)^\\s*(?:Type\\s*:\\s*AWS::Events::(Rule|Archive)|resource\\s+\"aws_cloudwatch_event_(rule|target|archive)\")");
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".json") || name.endsWith(".tf"))) continue;
            try {
                String content = java.nio.file.Files.readString(file);
                Matcher matcher = resource.matcher(content);
                while (matcher.find()) {
                    String kind = first(matcher.group(1), matcher.group(2));
                    DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "eventbridge-configuration", input.rootDirectory(), file,
                            ContractDiscoverySupport.relative(input.rootDirectory(), file) + ":" + kind,
                            ContractDiscoverySupport.line(content, matcher.group()),
                            DiscoveryConfidence.observedFact("An EventBridge rule, target, or archive resource is explicitly configured."), true, List.of(),
                            ContractDiscoverySupport.details("resourceKind", kind.toLowerCase(Locale.ROOT), "frameworkMarker", matcher.group().trim()));
                    evidence.add(item);
                    observations.add(ContractDiscoverySupport.observation(ID, "eventbridge-configuration-declared",
                            "EventBridge " + kind.toLowerCase(Locale.ROOT) + " configuration is declared in " + item.attributes().get("filePath") + ".",
                            item.confidence(), List.of(item)));
                }
            } catch (Exception ignored) { }
        }
    }

    private static DiscoveryEvidence compatibility(DiscoveryInput input, ContractDiscoverySupport.Document document,
                                                   DiscoveryEvidence contract, String mode) {
        new ContractCompatibilityEvidence(ContractId.of(contract.attributes().get("contractId")), mode, "", false,
                DiscoveryConfidence.observedFact("Compatibility metadata is explicit."));
        return ContractDiscoverySupport.evidence(ID, "contract-compatibility", input.rootDirectory(), document.absolutePath(),
                contract.identity() + ":" + mode, ContractDiscoverySupport.line(document.content(), mode),
                DiscoveryConfidence.observedFact("Compatibility metadata is explicitly declared."), true, List.of(contract.evidenceId()),
                ContractDiscoverySupport.details("contractId", contract.attributes().get("contractId"), "compatibility", mode));
    }

    private static MessageChannelType channelType(String name) {
        String value = name.toLowerCase(Locale.ROOT);
        if (value.contains("queue")) return MessageChannelType.QUEUE;
        if (value.contains("bus")) return MessageChannelType.EVENT_BUS;
        return MessageChannelType.TOPIC;
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

    private static boolean avroField(JsonNode root, String name) {
        for (JsonNode field : root.path("fields")) if (name.equals(field.path("name").asText())) return true;
        return false;
    }

    private static String versionFromJava(SpringDiscoverySupport.Source source) {
        Matcher matcher = Pattern.compile("(?i)(?:schema)?version\\s*[=(]\\s*\"?([0-9]+(?:\\.[0-9]+){0,2})").matcher(source.content());
        if (matcher.find()) return matcher.group(1);
        return ContractDiscoverySupport.pathVersion(source.path());
    }

    private static String listenerChannel(DiscoveryEvidence spring) {
        return switch (spring.attributes().getOrDefault("listenerKind", "")) {
            case "KafkaListener" -> MessageChannelType.TOPIC.name();
            case "RabbitListener", "SqsListener" -> MessageChannelType.QUEUE.name();
            default -> MessageChannelType.UNKNOWN.name();
        };
    }

    private static int parseLine(String value) { try { return Integer.parseInt(value); } catch (Exception ignored) { return 1; } }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
