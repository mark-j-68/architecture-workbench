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

/** Builds deterministic communication edges from exact producer, consumer, contract, and channel evidence. */
public class MessagingTopologyDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("messaging.topology");
    private static final Pattern INFRA_MARKER = Pattern.compile("(?im)(deadLetterTargetArn|redrivePolicy|dead[-_. ]?letter|retry[-_. ]?queue|archiveName|AWS::Events::Archive|aws_cloudwatch_event_archive|AWS::SNS::Subscription|aws_sns_topic_subscription)\\s*[:=]?\\s*\"?([^\\s\",}]+)?");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Messaging Topology Discovery Plugin", "0.2.4", "Messaging Plugin",
                List.of("eventbridge", "sqs", "sns", "kafka", "rabbitmq", "asyncapi"),
                List.of(DiscoveryPluginCapability.DETECT_MESSAGING_TOPOLOGY),
                List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(EventContractDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(CommandContractDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(SpringMessagingDiscoveryPlugin.ID, false)), true);
    }

    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        List<DiscoveryEvidence> prior = ContractDiscoverySupport.priorEvidence(input);
        for (DiscoveryEvidence item : prior) {
            switch (item.evidenceType()) {
                case "event-producer-reference", "command-producer-reference", "asyncapi-producer" ->
                        producerEdge(input, item, evidence, observations);
                case "event-consumer-reference", "command-consumer-reference", "asyncapi-consumer" ->
                        consumerEdge(input, item, evidence, observations);
                case "spring-message-listener" -> springConsumerEdge(input, item, evidence, observations);
                default -> { }
            }
        }
        handlerCommandEdges(input, prior, evidence, observations, diagnostics);
        infrastructureEdges(input, evidence, observations, diagnostics);
        return ContractDiscoverySupport.result(ID, started, evidence, observations, diagnostics, false);
    }

    private static void producerEdge(DiscoveryInput input, DiscoveryEvidence source, List<DiscoveryEvidence> evidence,
                                     List<DiscoveryObservation> observations) {
        String producer = first(source.attributes().get("producer"), source.attributes().get("symbol"));
        String channel = first(source.attributes().get("channelName"), source.attributes().get("queueName"));
        if (producer.isBlank() || channel.isBlank()) return;
        String contract = first(source.attributes().get("eventName"), source.attributes().get("commandName"), source.attributes().get("contractId"));
        if (contract.isBlank()) contract = channel;
        ProducerReference reference = new ProducerReference(source.attributes().getOrDefault("module", "."), producer,
                ContractId.of(contract), new MessageChannel(channel, channelType(source, channel), ContractDiscoverySupport.dynamic(channel)));
        DiscoveryEvidence edge = relation(input, "topology-producer-channel", source, producer + "->" + channel,
                ContractDiscoverySupport.details("from", reference.symbol(), "to", reference.channel().name(), "relationship", "PRODUCES_TO",
                        "contractId", reference.contractId().value(), "channelName", reference.channel().name(), "channelType", reference.channel().type().name()));
        evidence.add(edge);
        observations.add(ContractDiscoverySupport.observation(ID, "messaging-producer-channel-observed",
                producer + " produces " + (contract.isBlank() ? "messages" : contract) + " to " + channel + ".",
                edge.confidence(), List.of(edge)));
    }

    private static void consumerEdge(DiscoveryInput input, DiscoveryEvidence source, List<DiscoveryEvidence> evidence,
                                     List<DiscoveryObservation> observations) {
        String consumer = first(source.attributes().get("consumer"), source.attributes().get("symbol"));
        String channel = first(source.attributes().get("channelName"), source.attributes().get("queueName"));
        String contract = first(source.attributes().get("eventName"), source.attributes().get("commandName"), source.attributes().get("contractId"));
        if (!channel.isBlank()) {
            if (contract.isBlank()) contract = channel;
            ConsumerReference reference = new ConsumerReference(source.attributes().getOrDefault("module", "."), consumer,
                    ContractId.of(contract), new MessageChannel(channel, channelType(source, channel), ContractDiscoverySupport.dynamic(channel)));
            DiscoveryEvidence edge = relation(input, "topology-channel-consumer", source, channel + "->" + consumer,
                    ContractDiscoverySupport.details("from", reference.channel().name(), "to", reference.symbol(), "relationship", "CONSUMED_BY",
                            "contractId", reference.contractId().value(), "channelName", reference.channel().name(), "channelType", reference.channel().type().name()));
            evidence.add(edge);
            observations.add(ContractDiscoverySupport.observation(ID, "messaging-channel-consumer-observed",
                    channel + " is consumed by " + consumer + ".", edge.confidence(), List.of(edge)));
        }
        if (!contract.isBlank() && !consumer.isBlank()) eventHandlerEdge(input, source, contract, consumer, evidence, observations);
    }

    private static void springConsumerEdge(DiscoveryInput input, DiscoveryEvidence source, List<DiscoveryEvidence> evidence,
                                           List<DiscoveryObservation> observations) {
        String consumer = source.attributes().getOrDefault("symbol", source.identity());
        String channel = source.attributes().getOrDefault("destination", "");
        String contract = source.attributes().getOrDefault("eventType", "");
        if (!channel.isBlank()) {
            DiscoveryEvidence edge = relation(input, "topology-channel-consumer", source, channel + "->" + consumer,
                    ContractDiscoverySupport.details("from", channel, "to", consumer, "relationship", "CONSUMED_BY",
                            "contractId", contract, "channelName", channel, "channelType", listenerChannelType(source).name()));
            evidence.add(edge);
            observations.add(ContractDiscoverySupport.observation(ID, "messaging-channel-consumer-observed",
                    channel + " is consumed by " + consumer + ".", edge.confidence(), List.of(edge)));
        }
        if (!contract.isBlank()) eventHandlerEdge(input, source, contract, consumer, evidence, observations);
    }

    private static void eventHandlerEdge(DiscoveryInput input, DiscoveryEvidence source, String contract, String consumer,
                                         List<DiscoveryEvidence> evidence, List<DiscoveryObservation> observations) {
        DiscoveryEvidence edge = relation(input, "topology-event-handler", source, contract + "->" + consumer,
                ContractDiscoverySupport.details("from", contract, "to", consumer, "relationship", "HANDLED_BY", "contractId", contract));
        evidence.add(edge);
        observations.add(ContractDiscoverySupport.observation(ID, "messaging-event-handler-observed",
                consumer + " consumes " + contract + ".", edge.confidence(), List.of(edge)));
    }

    private static void handlerCommandEdges(DiscoveryInput input, List<DiscoveryEvidence> prior, List<DiscoveryEvidence> evidence,
                                            List<DiscoveryObservation> observations, List<String> diagnostics) {
        Map<String, DiscoveryEvidence> handlersByClass = new LinkedHashMap<>();
        for (DiscoveryEvidence item : prior) {
            if (item.evidenceType().equals("spring-message-listener") || item.evidenceType().equals("event-consumer-reference"))
                handlersByClass.put(item.attributes().getOrDefault("className", ""), item);
        }
        Pattern commandCreation = Pattern.compile("new\\s+([A-Za-z_$][\\w$]*Command)\\s*\\(");
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics)) {
            DiscoveryEvidence handler = handlersByClass.get(source.className());
            if (handler == null && !(source.className().contains("Handler") || source.className().contains("Policy") || source.className().contains("Router"))) continue;
            Matcher matcher = commandCreation.matcher(source.content());
            while (matcher.find()) {
                String command = matcher.group(1);
                List<String> sourceIds = handler == null ? List.of() : List.of(handler.evidenceId());
                DiscoveryEvidence edge = ContractDiscoverySupport.sourceEvidence(ID, "topology-handler-command", source,
                        source.qualifiedName() + "->" + command, ContractDiscoverySupport.line(source.content(), matcher.group()),
                        DiscoveryConfidence.high("The handler directly constructs a convention-labelled command type."), false, sourceIds,
                        ContractDiscoverySupport.details("from", source.qualifiedName(), "to", command, "relationship", "PUBLISHES_COMMAND",
                                "contractId", command, "uncertainty", "command-type-correlated-by-exact-name"));
                evidence.add(edge);
                observations.add(ContractDiscoverySupport.observation(ID, "messaging-handler-command-observed",
                        source.className() + " references command " + command + ".", edge.confidence(), List.of(edge)));
            }
        }
    }

    private static void infrastructureEdges(DiscoveryInput input, List<DiscoveryEvidence> evidence,
                                            List<DiscoveryObservation> observations, List<String> diagnostics) {
        for (Path file : ContractDiscoverySupport.files(input.rootDirectory(), diagnostics)) {
            String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".json")
                    || lower.endsWith(".tf") || lower.endsWith(".properties"))) continue;
            try {
                String content = Files.readString(file);
                Matcher matcher = INFRA_MARKER.matcher(content);
                while (matcher.find()) {
                    String marker = matcher.group(1);
                    String value = matcher.group(2) == null ? "" : matcher.group(2);
                    String kind = infrastructureKind(marker);
                    DiscoveryEvidence item = ContractDiscoverySupport.evidence(ID, "messaging-infrastructure-topology", input.rootDirectory(), file,
                            marker + ":" + value, ContractDiscoverySupport.line(content, matcher.group()),
                            DiscoveryConfidence.observedFact("Messaging infrastructure configuration is explicitly present."), true, List.of(),
                            ContractDiscoverySupport.details("topologyKind", kind, "configurationKey", marker, "channelName", value,
                                    "relationship", kind.equals("sns-sqs-fanout") ? "FAN_OUT_TO" : "CONFIGURES"));
                    evidence.add(item);
                    observations.add(ContractDiscoverySupport.observation(ID, "messaging-infrastructure-observed",
                            kind + " configuration is declared in " + item.attributes().get("filePath") + ".", item.confidence(), List.of(item)));
                }
            } catch (Exception exception) {
                diagnostics.add("Unable to inspect messaging configuration " + ContractDiscoverySupport.relative(input.rootDirectory(), file)
                        + ": " + exception.getMessage());
            }
        }
    }

    private static DiscoveryEvidence relation(DiscoveryInput input, String type, DiscoveryEvidence source,
                                              String identity, Map<String, String> details) {
        Path file = input.rootDirectory().resolve(source.attributes().getOrDefault("filePath", source.references().get(0))).normalize();
        int line = parseLine(source.attributes().get("line"));
        boolean observed = source.directlyObserved();
        DiscoveryConfidence confidence = source.confidence().value() < 0.9
                ? new DiscoveryConfidence(source.confidence().value(), "The topology edge retains reduced confidence from its source: " + source.confidence().rationale())
                : observed ? DiscoveryConfidence.observedFact("The topology edge is copied from an explicit producer or consumer declaration.")
                : DiscoveryConfidence.high("The topology edge is deterministically composed from prior evidence.");
        Map<String, String> relationshipDetails = new LinkedHashMap<>(details);
        relationshipDetails.putIfAbsent("uncertainty", source.attributes().getOrDefault("uncertainty", ""));
        return ContractDiscoverySupport.evidence(ID, type, input.rootDirectory(), file, identity, line, confidence, observed,
                List.of(source.evidenceId()), relationshipDetails);
    }

    private static MessageChannelType channelType(DiscoveryEvidence source, String channel) {
        try { return MessageChannelType.valueOf(source.attributes().getOrDefault("channelType", "UNKNOWN")); }
        catch (IllegalArgumentException ignored) {
            String value = channel.toLowerCase(Locale.ROOT);
            return value.contains("queue") ? MessageChannelType.QUEUE : value.contains("bus") ? MessageChannelType.EVENT_BUS : MessageChannelType.TOPIC;
        }
    }

    private static MessageChannelType listenerChannelType(DiscoveryEvidence source) {
        return switch (source.attributes().getOrDefault("listenerKind", "")) {
            case "SqsListener" -> MessageChannelType.QUEUE;
            case "RabbitListener" -> MessageChannelType.QUEUE;
            case "KafkaListener" -> MessageChannelType.TOPIC;
            default -> MessageChannelType.UNKNOWN;
        };
    }

    private static String infrastructureKind(String marker) {
        String value = marker.toLowerCase(Locale.ROOT);
        if (value.contains("archive")) return "archive-replay";
        if (value.contains("subscription")) return "sns-sqs-fanout";
        if (value.contains("retry")) return "retry-queue";
        return "dead-letter-queue";
    }

    private static int parseLine(String value) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return 1; }
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
