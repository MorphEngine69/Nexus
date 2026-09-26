package com.morphengine.nexus.terminal;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * How large a terminal's panel is. Each size asks for a number of columns and,
 * per {@link TerminalKind}, of rows; the panel gets as much of that as fits the
 * window. {@link #FULL} takes the whole height of the window.
 */
public enum TerminalSize implements StringRepresentable {

    SMALL(9),
    MEDIUM(11),
    LARGE(13),
    FULL(16);

    public static final StringRepresentable.EnumCodec<TerminalSize> CODEC =
            StringRepresentable.fromEnum(TerminalSize::values);

    private final int columns;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    TerminalSize(final int columns) {
        this.columns = columns;
    }

    /**
     * @return columns of resources asked for; never fewer than a row of the player's inventory
     */
    public int columns() {
        return columns;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
