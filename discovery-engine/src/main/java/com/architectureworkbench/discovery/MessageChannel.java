package com.architectureworkbench.discovery;

import java.util.Objects;

public record MessageChannel(String name, MessageChannelType type, boolean dynamic) {
    public MessageChannel {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Channel name is required.");
        name = name.trim();
        type = Objects.requireNonNullElse(type, MessageChannelType.UNKNOWN);
    }
}
