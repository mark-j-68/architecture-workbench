package com.architectureworkbench.core.audit;

import com.architectureworkbench.core.model.audit.ActivityEnvelope;
import com.architectureworkbench.core.model.audit.ProtectedPayloadReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ImmutableActivityLog {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final List<ActivityEnvelope> envelopes = new ArrayList<>();
    private final ProtectedPayloadStore payloadStore;

    public ImmutableActivityLog(ProtectedPayloadStore payloadStore) {
        this.payloadStore = payloadStore;
    }

    public synchronized ActivityEnvelope append(String workspaceId, String actorRef, String action, String correlationId, String protectedPayload) {
        ProtectedPayloadReference ref = payloadStore.store(workspaceId, correlationId, "AI_REVIEW_TRACE", protectedPayload);

        ActivityEnvelope envelope = new ActivityEnvelope();
        envelope.setActivityId("act-" + UUID.randomUUID());
        envelope.setOccurredAt(Instant.now());
        envelope.setActorRef(actorRef);
        envelope.setAction(action);
        envelope.setWorkspaceId(workspaceId);
        envelope.setCorrelationId(correlationId);
        envelope.setPreviousHash(envelopes.isEmpty() ? "GENESIS" : envelopes.get(envelopes.size() - 1).getEnvelopeHash());
        envelope.setPayloadHash(sha256(Objects.requireNonNullElse(protectedPayload, "")));
        envelope.getProtectedPayloads().add(ref);
        envelope.setEnvelopeHash(hashEnvelope(envelope));
        envelopes.add(envelope);
        return envelope;
    }

    public synchronized List<ActivityEnvelope> entriesForWorkspace(String workspaceId) {
        return envelopes.stream()
                .filter(envelope -> Objects.equals(envelope.getWorkspaceId(), workspaceId))
                .toList();
    }

    public ProtectedPayloadStore getPayloadStore() {
        return payloadStore;
    }

    private static String hashEnvelope(ActivityEnvelope envelope) {
        try {
            return sha256(MAPPER.writeValueAsString(new EnvelopeHashInput(
                    envelope.getActivityId(),
                    envelope.getOccurredAt(),
                    envelope.getActorRef(),
                    envelope.getAction(),
                    envelope.getWorkspaceId(),
                    envelope.getCorrelationId(),
                    envelope.getPreviousHash(),
                    envelope.getPayloadHash(),
                    envelope.getProtectedPayloads()
            )));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to hash audit envelope", e);
        }
    }

    private static String sha256(String value) {
        try {
            return bytesToHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private record EnvelopeHashInput(
            String activityId,
            Instant occurredAt,
            String actorRef,
            String action,
            String workspaceId,
            String correlationId,
            String previousHash,
            String payloadHash,
            List<ProtectedPayloadReference> protectedPayloads
    ) {}
}
