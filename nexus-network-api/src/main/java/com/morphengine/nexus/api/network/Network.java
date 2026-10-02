package com.morphengine.nexus.api.network;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a network: an id that never changes, its display name and its
 * identification color. The id tells networks apart for good, whatever they
 * are called and wherever their controller stands; names may repeat.
 * Membership (which nodes belong to it) is a graph concern, computed
 * separately via {@link NetworkGraphs}, not tracked here.
 */
public final class Network {

    public static final int MAX_NAME_LENGTH = 32;

    private final UUID id;
    private String name;
    private NetworkColor color;

    public Network(final UUID id, final String name, final NetworkColor color) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = validateName(name);
        this.color = Objects.requireNonNull(color, "color must not be null");
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public NetworkColor color() {
        return color;
    }

    public void rename(final String newName) {
        this.name = validateName(newName);
    }

    public void recolor(final NetworkColor newColor) {
        this.color = Objects.requireNonNull(newColor, "color must not be null");
    }

    private static String validateName(final String candidate) {
        Objects.requireNonNull(candidate, "name must not be null");
        final String trimmed = candidate.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("network name must not be blank");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "network name longer than " + MAX_NAME_LENGTH + " characters: " + trimmed);
        }
        return trimmed;
    }
}
