package com.morphengine.nexus.terminal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.networking.NullableStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * How a terminal lists the network's storage, chosen with the buttons beside
 * its panel and kept by the terminal, so the choice survives leaving the world.
 *
 * @param shownType id of the only resource type shown; {@code null} shows every type
 */
public record TerminalSettings(
        SortOrder sort, SortDirection direction, @Nullable Identifier shownType, TerminalSize size) {

    public static final TerminalSettings DEFAULT =
            new TerminalSettings(SortOrder.AMOUNT, SortDirection.DESCENDING, null, TerminalSize.MEDIUM);

    public static final Codec<TerminalSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    SortOrder.CODEC.optionalFieldOf("sort", DEFAULT.sort()).forGetter(TerminalSettings::sort),
                    SortDirection.CODEC.optionalFieldOf("direction", DEFAULT.direction())
                            .forGetter(TerminalSettings::direction),
                    Identifier.CODEC.optionalFieldOf("shown_type")
                            .forGetter(settings -> Optional.ofNullable(settings.shownType())),
                    TerminalSize.CODEC.optionalFieldOf("size", DEFAULT.size()).forGetter(TerminalSettings::size))
            .apply(instance, (sort, direction, shownType, size) ->
                    new TerminalSettings(sort, direction, shownType.orElse(null), size)));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSettings> STREAM_CODEC =
            StreamCodec.composite(
                    NeoForgeStreamCodecs.enumCodec(SortOrder.class), TerminalSettings::sort,
                    NeoForgeStreamCodecs.enumCodec(SortDirection.class), TerminalSettings::direction,
                    NullableStreamCodec.of(Identifier.STREAM_CODEC), TerminalSettings::shownType,
                    NeoForgeStreamCodecs.enumCodec(TerminalSize.class), TerminalSettings::size,
                    TerminalSettings::new);

    public TerminalSettings {
        Objects.requireNonNull(sort, "sort must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(size, "size must not be null");
    }

    public TerminalSettings withSort(final SortOrder newSort) {
        return new TerminalSettings(newSort, direction, shownType, size);
    }

    public TerminalSettings withDirection(final SortDirection newDirection) {
        return new TerminalSettings(sort, newDirection, shownType, size);
    }

    public TerminalSettings withShownType(final @Nullable Identifier newShownType) {
        return new TerminalSettings(sort, direction, newShownType, size);
    }

    public TerminalSettings withSize(final TerminalSize newSize) {
        return new TerminalSettings(sort, direction, shownType, newSize);
    }
}
