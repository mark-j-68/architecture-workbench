package com.architectureworkbench.core.model.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, non-PII audit envelope. Sensitive payloads must be stored via encrypted references.
 */
public class ActivityEnvelope {
    private String activityId;
    private Instant occurredAt = Instant.now();
    private String actorRef;
    private String action;
    private String workspaceId;
    private String correlationId;
    private String previousHash;
    private String payloadHash;
    private String envelopeHash;
    private List<ProtectedPayloadReference> protectedPayloads = new ArrayList<>();

    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getActorRef() { return actorRef; }
    public void setActorRef(String actorRef) { this.actorRef = actorRef; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getEnvelopeHash() { return envelopeHash; }
    public void setEnvelopeHash(String envelopeHash) { this.envelopeHash = envelopeHash; }
    public List<ProtectedPayloadReference> getProtectedPayloads() { return protectedPayloads; }
    public void setProtectedPayloads(List<ProtectedPayloadReference> protectedPayloads) { this.protectedPayloads = Objects.requireNonNullElseGet(protectedPayloads, ArrayList::new); }
}
