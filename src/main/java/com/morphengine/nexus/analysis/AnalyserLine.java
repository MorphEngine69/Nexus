package com.morphengine.nexus.analysis;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * One line of what an Analyser tells about a block: a name and its value, or, with no value, a heading.
 *
 * @param label what is told about
 * @param value what it is now; empty for a heading
 */
public record AnalyserLine(Component label, Component value) {

    public AnalyserLine {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    public boolean isHeading() {
        return value.getString().isEmpty();
    }
}
