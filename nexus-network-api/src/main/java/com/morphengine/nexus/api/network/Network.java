package com.morphengine.nexus.api.network;

import java.util.Objects;

/**
 * Identity of a network: its display name and identification color. Membership
 * (which nodes belong to it) is a graph concern, computed separately via
 * {@link NetworkGraphs}, not tracked here.
 */
public final class Network {
    

    public static final int MAX_NAME_LENGTH = 32;

    private String name;
    private NetworkColor color;

    public Network(final String name, final NetworkColor color) {
        this.name = validateName(name);
        this.color = Objects.requireNonNull(color, "color must not be null");
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
