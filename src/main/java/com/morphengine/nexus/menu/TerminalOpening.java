package com.morphengine.nexus.menu;

import com.morphengine.nexus.terminal.TerminalSettings;

import java.util.Objects;

/**
 * What a terminal menu opens with: what it is bound to, and the settings the
 * player left it with.
 */
public record TerminalOpening(TerminalBinding binding, TerminalSettings settings) {

    public TerminalOpening {
        Objects.requireNonNull(binding, "binding must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
    }
}
