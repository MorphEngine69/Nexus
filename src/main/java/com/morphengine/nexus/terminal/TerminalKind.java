package com.morphengine.nexus.terminal;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The terminals: the plain one lists the network's storage, the crafting one
 * adds a crafting grid below the list and so asks for fewer rows at each size.
 */
public enum TerminalKind implements StringRepresentable {

    TERMINAL(5, 9, 13),
    CRAFTING_TERMINAL(3, 6, 9);

    public static final Codec<TerminalKind> CODEC = StringRepresentable.fromEnum(TerminalKind::values);

    private final int smallRows;
    private final int mediumRows;
    private final int largeRows;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    TerminalKind(final int smallRows, final int mediumRows, final int largeRows) {
        this.smallRows = smallRows;
        this.mediumRows = mediumRows;
        this.largeRows = largeRows;
    }

    public boolean hasCraftingGrid() {
        return this == CRAFTING_TERMINAL;
    }

    /**
     * @return rows of resources asked for at {@code size}; {@link Integer#MAX_VALUE}
     *         for {@link TerminalSize#FULL}, which takes as many as fit
     */
    public int rows(final TerminalSize size) {
        return switch (size) {
            case SMALL -> smallRows;
            case MEDIUM -> mediumRows;
            case LARGE -> largeRows;
            case FULL -> Integer.MAX_VALUE;
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
