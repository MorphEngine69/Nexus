package com.morphengine.nexus.probe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/**
 * One line of what a tooltip mod such as Jade or The One Probe shows for a block: plain text, or a bar for energy, a
 * tank or progress. It says what to show and not how, so each mod draws it in its own way.
 */
public sealed interface ProbeLine {

    StreamCodec<RegistryFriendlyByteBuf, ProbeLine> STREAM_CODEC = StreamCodec.of(ProbeLine::encode, ProbeLine::decode);

    private static void requireNotNegative(final long value, final String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
    }

    private static void encode(final RegistryFriendlyByteBuf buffer, final ProbeLine line) {
        switch (line) {
            case Text text -> {
                buffer.writeByte(Kind.TEXT.ordinal());
                ComponentSerialization.STREAM_CODEC.encode(buffer, text.text());
            }
            case Energy energy -> {
                buffer.writeByte(Kind.ENERGY.ordinal());
                buffer.writeVarLong(energy.stored());
                buffer.writeVarLong(energy.capacity());
            }
            case Tank tank -> {
                buffer.writeByte(Kind.TANK.ordinal());
                FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, tank.contents());
                buffer.writeVarLong(tank.capacity());
                ComponentSerialization.STREAM_CODEC.encode(buffer, tank.name());
            }
            case Progress progress -> {
                buffer.writeByte(Kind.PROGRESS.ordinal());
                ComponentSerialization.STREAM_CODEC.encode(buffer, progress.label());
                ByteBufCodecs.VAR_INT.encode(buffer, progress.percent());
            }
        }
    }

    private static ProbeLine decode(final RegistryFriendlyByteBuf buffer) {
        final Kind[] kinds = Kind.values();
        final int index = buffer.readByte();
        if (index < 0 || index >= kinds.length) {
            throw new IllegalArgumentException("no probe line of kind " + index);
        }
        return switch (kinds[index]) {
            case TEXT -> new Text(ComponentSerialization.STREAM_CODEC.decode(buffer));
            case ENERGY -> new Energy(buffer.readVarLong(), buffer.readVarLong());
            case TANK -> new Tank(FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readVarLong(),
                    ComponentSerialization.STREAM_CODEC.decode(buffer));
            case PROGRESS -> new Progress(ComponentSerialization.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer));
        };
    }

    /**
     * A line of text.
     */
    record Text(Component text) implements ProbeLine {

        public Text {
            Objects.requireNonNull(text, "text must not be null");
        }
    }

    /**
     * The FE stored in a buffer and what the buffer holds.
     */
    record Energy(long stored, long capacity) implements ProbeLine {

        public Energy {
            requireNotNegative(stored, "stored");
            requireNotNegative(capacity, "capacity");
        }
    }

    /**
     * A tank, in millibuckets.
     *
     * @param contents what the tank holds; empty for an empty tank
     * @param capacity what the tank holds at most
     * @param name     what to call the tank: the fluid it holds, or the one it takes when it holds none
     */
    record Tank(FluidStack contents, long capacity, Component name) implements ProbeLine {

        public Tank {
            Objects.requireNonNull(contents, "contents must not be null");
            Objects.requireNonNull(name, "name must not be null");
            requireNotNegative(capacity, "capacity");
            contents = contents.copy();
        }
    }

    /**
     * How far some work is.
     *
     * @param percent from 0 to 100
     */
    record Progress(Component label, int percent) implements ProbeLine {

        private static final int FULL = 100;

        public Progress {
            Objects.requireNonNull(label, "label must not be null");
            percent = Math.clamp(percent, 0, FULL);
        }
    }

    /** The kinds of line, in the order they are written. */
    enum Kind {
        TEXT,
        ENERGY,
        TANK,
        PROGRESS
    }
}
