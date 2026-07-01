package com.architectureworkbench.core.model.capture;

/** Canonical Event Storming sticky types recognised by Architecture Workbench. */
public enum EventStormStickyType {
    DOMAIN_EVENT,
    COMMAND,
    AGGREGATE,
    POLICY,
    READ_MODEL,
    EXTERNAL_SYSTEM,
    USER_ROLE,
    HOTSPOT,
    COMMENT,
    UNKNOWN
}
