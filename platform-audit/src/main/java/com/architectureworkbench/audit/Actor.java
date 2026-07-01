package com.architectureworkbench.audit;

import java.util.Objects;

public record Actor(String actorRef, ActorType type) {
    public Actor {
        actorRef = Objects.requireNonNullElse(actorRef, "system");
        type = Objects.requireNonNullElse(type, ActorType.SYSTEM);
    }

    public static Actor system(String actorRef) {
        return new Actor(actorRef, ActorType.SYSTEM);
    }

    public static Actor human(String actorRef) {
        return new Actor(actorRef, ActorType.HUMAN);
    }

    public static Actor service(String actorRef) {
        return new Actor(actorRef, ActorType.SERVICE);
    }

    public static Actor reviewer(String actorRef) {
        return new Actor(actorRef, ActorType.REVIEWER);
    }
}
