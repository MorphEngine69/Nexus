package com.morphengine.nexus.analysis;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Collects the lines an Analyser shows about one block. Names are keys under {@code gui.nexus.analyser.}, so that
 * every line is in the language of the player who reads it. Past {@value #MAX_LINES} lines nothing more is kept.
 */
public final class AnalyserLines {

    /** The most lines one report carries. */
    public static final int MAX_LINES = 40;

    private static final String KEY_PREFIX = "gui.nexus.analyser.";
    private static final int TICKS_PER_SECOND = 20;

    private final List<AnalyserLine> lines = new ArrayList<>();

    public AnalyserLines heading(final String key) {
        return append(new AnalyserLine(Component.translatable(KEY_PREFIX + key), Component.empty()));
    }

    public AnalyserLines add(final String key, final Component value) {
        return append(new AnalyserLine(Component.translatable(KEY_PREFIX + key), value));
    }

    public AnalyserLines add(final String key, final String text) {
        return add(key, Component.literal(text));
    }

    /**
     * Adds a line named by something other than a key, such as the name of an item.
     */
    public AnalyserLines line(final Component label, final Component value) {
        return append(new AnalyserLine(label, value));
    }

    /**
     * Adds a line whose value is the text under {@code valueKey}, filled with {@code arguments}.
     */
    public AnalyserLines addTranslated(final String key, final String valueKey, final Object... arguments) {
        return add(key, Component.translatable(KEY_PREFIX + valueKey, arguments));
    }

    /**
     * @return the lines kept, in the order they were added; a view that does not change when more are added
     */
    public List<AnalyserLine> lines() {
        return List.copyOf(lines);
    }

    /**
     * @return a number of FE per second as FE per tick, to two decimals
     */
    public static String perTick(final long perSecond) {
        return String.format(Locale.ROOT, "%,.2f", (double) perSecond / TICKS_PER_SECOND);
    }

    /**
     * @return a number of FE per second as FE per tick with its sign, to two decimals
     */
    public static String signedPerTick(final long perSecond) {
        return String.format(Locale.ROOT, "%+,.2f", (double) perSecond / TICKS_PER_SECOND);
    }

    private AnalyserLines append(final AnalyserLine line) {
        if (lines.size() < MAX_LINES) {
            lines.add(line);
        }
        return this;
    }
}
