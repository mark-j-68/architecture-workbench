package com.architectureworkbench.core.model.governance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PiiProtectionPolicy {
    private boolean piiClassificationRequired = true;
    private boolean encryptionRequired = true;
    private String encryptionScope = "PER_SUBJECT_OR_CASE";
    private boolean cryptographicShreddingRequired = true;
    private String keyDestructionWorkflow = "DELETE_DATA_ENCRYPTION_KEY_OR_DESTROY_WRAPPED_KEY";
    private List<String> piiCategories = new ArrayList<>();

    public boolean isPiiClassificationRequired() { return piiClassificationRequired; }
    public void setPiiClassificationRequired(boolean piiClassificationRequired) { this.piiClassificationRequired = piiClassificationRequired; }
    public boolean isEncryptionRequired() { return encryptionRequired; }
    public void setEncryptionRequired(boolean encryptionRequired) { this.encryptionRequired = encryptionRequired; }
    public String getEncryptionScope() { return encryptionScope; }
    public void setEncryptionScope(String encryptionScope) { this.encryptionScope = encryptionScope; }
    public boolean isCryptographicShreddingRequired() { return cryptographicShreddingRequired; }
    public void setCryptographicShreddingRequired(boolean cryptographicShreddingRequired) { this.cryptographicShreddingRequired = cryptographicShreddingRequired; }
    public String getKeyDestructionWorkflow() { return keyDestructionWorkflow; }
    public void setKeyDestructionWorkflow(String keyDestructionWorkflow) { this.keyDestructionWorkflow = keyDestructionWorkflow; }
    public List<String> getPiiCategories() { return piiCategories; }
    public void setPiiCategories(List<String> piiCategories) { this.piiCategories = Objects.requireNonNullElseGet(piiCategories, ArrayList::new); }
}
