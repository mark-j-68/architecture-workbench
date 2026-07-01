package com.architectureworkbench.core.model.governance;

public class AuditGovernanceSection {
    private boolean immutableActivityLogRequired = true;
    private String appendOnlyStore = "S3_OBJECT_LOCK";
    private boolean hashChainRequired = true;
    private boolean piiAllowedInImmutableEnvelope = false;
    private String payloadStrategy = "ENCRYPTED_EXTERNAL_PAYLOAD_REFERENCE";
    private String retentionPolicy = "REGULATORY_RETENTION_POLICY";

    public boolean isImmutableActivityLogRequired() { return immutableActivityLogRequired; }
    public void setImmutableActivityLogRequired(boolean immutableActivityLogRequired) { this.immutableActivityLogRequired = immutableActivityLogRequired; }
    public String getAppendOnlyStore() { return appendOnlyStore; }
    public void setAppendOnlyStore(String appendOnlyStore) { this.appendOnlyStore = appendOnlyStore; }
    public boolean isHashChainRequired() { return hashChainRequired; }
    public void setHashChainRequired(boolean hashChainRequired) { this.hashChainRequired = hashChainRequired; }
    public boolean isPiiAllowedInImmutableEnvelope() { return piiAllowedInImmutableEnvelope; }
    public void setPiiAllowedInImmutableEnvelope(boolean piiAllowedInImmutableEnvelope) { this.piiAllowedInImmutableEnvelope = piiAllowedInImmutableEnvelope; }
    public String getPayloadStrategy() { return payloadStrategy; }
    public void setPayloadStrategy(String payloadStrategy) { this.payloadStrategy = payloadStrategy; }
    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }
}
