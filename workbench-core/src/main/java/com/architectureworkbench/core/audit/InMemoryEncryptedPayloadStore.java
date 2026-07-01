package com.architectureworkbench.core.audit;

import com.architectureworkbench.core.model.audit.ProtectedPayloadReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-friendly protected payload store. Production adapters should replace this with envelope encryption
 * backed by external key management and durable object storage.
 */
public class InMemoryEncryptedPayloadStore implements ProtectedPayloadStore {
    private final Map<String, StoredPayload> payloads = new ConcurrentHashMap<>();

    @Override
    public ProtectedPayloadReference store(String workspaceId, String correlationId, String classification, String payload) {
        String payloadId = "pp-" + UUID.randomUUID();
        String keyRef = "kms://architecture-workbench/%s/%s".formatted(workspaceId, payloadId);
        String ciphertext = Base64.getEncoder().encodeToString(Objects.requireNonNullElse(payload, "").getBytes(StandardCharsets.UTF_8));
        payloads.put(payloadId, new StoredPayload(ciphertext, keyRef));

        ProtectedPayloadReference ref = new ProtectedPayloadReference();
        ref.setPayloadId(payloadId);
        ref.setStorageUri("memory://protected-payloads/" + payloadId);
        ref.setEncryptionKeyRef(keyRef);
        ref.setClassification(classification);
        ref.setCryptoShreddable(true);
        return ref;
    }

    @Override
    public String retrieve(String payloadId) {
        StoredPayload stored = payloads.get(payloadId);
        if (stored == null || stored.shredded()) {
            return null;
        }
        return new String(Base64.getDecoder().decode(stored.ciphertext()), StandardCharsets.UTF_8);
    }

    @Override
    public boolean shred(String payloadId) {
        StoredPayload stored = payloads.get(payloadId);
        if (stored == null) {
            return false;
        }
        payloads.put(payloadId, new StoredPayload(sha256(stored.ciphertext()), stored.keyRef(), true));
        return true;
    }

    private static String sha256(String value) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    private record StoredPayload(String ciphertext, String keyRef, boolean shredded) {
        StoredPayload(String ciphertext, String keyRef) {
            this(ciphertext, keyRef, false);
        }
    }
}
