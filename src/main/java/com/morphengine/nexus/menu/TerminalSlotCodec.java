package com.morphengine.nexus.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;

/**
 * Writes a {@link TerminalSlot} as the kind it is, then what the kind needs.
 */
final class TerminalSlotCodec {

    static final StreamCodec<RegistryFriendlyByteBuf, TerminalSlot> CODEC =
            StreamCodec.of(TerminalSlotCodec::write, TerminalSlotCodec::read);

    private enum Kind {
        HAND,
        CARRIED,
        CURIO
    }

    private TerminalSlotCodec() {
    }

    private static void write(final RegistryFriendlyByteBuf buffer, final TerminalSlot slot) {
        switch (slot) {
            case TerminalSlot.Hand hand -> {
                buffer.writeEnum(Kind.HAND);
                buffer.writeEnum(hand.hand());
            }
            case TerminalSlot.Carried carried -> {
                buffer.writeEnum(Kind.CARRIED);
                buffer.writeVarInt(carried.index());
            }
            case TerminalSlot.Curio curio -> {
                buffer.writeEnum(Kind.CURIO);
                buffer.writeUtf(curio.slotType());
                buffer.writeVarInt(curio.index());
            }
        }
    }

    private static TerminalSlot read(final RegistryFriendlyByteBuf buffer) {
        return switch (buffer.readEnum(Kind.class)) {
            case HAND -> new TerminalSlot.Hand(buffer.readEnum(InteractionHand.class));
            case CARRIED -> new TerminalSlot.Carried(buffer.readVarInt());
            case CURIO -> new TerminalSlot.Curio(buffer.readUtf(), buffer.readVarInt());
        };
    }
}
