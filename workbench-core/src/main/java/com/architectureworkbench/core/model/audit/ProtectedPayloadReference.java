package com.architectureworkbench.core.model.audit;

public class ProtectedPayloadReference {
    private String payloadId;
    private String storageUri;
    private String encryptionKeyRef;
    private String classification;
    private boolean cryptoShreddable = true;

    public String getPayloadId() { return payloadId; }
    public void setPayloadId(String payloadId) { this.payloadId = payloadId; }
    public String getStorageUri() { return storageUri; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public String getEncryptionKeyRef() { return encryptionKeyRef; }
    public void setEncryptionKeyRef(String encryptionKeyRef) { this.encryptionKeyRef = encryptionKeyRef; }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public boolean isCryptoShreddable() { return cryptoShreddable; }
    public void setCryptoShreddable(boolean cryptoShreddable) { this.cryptoShreddable = cryptoShreddable; }
}
