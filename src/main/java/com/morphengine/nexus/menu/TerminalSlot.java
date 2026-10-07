package com.morphengine.nexus.menu;

import com.morphengine.nexus.integration.curios.CuriosTerminals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Where a Nexus Terminal is carried: in a hand, in a slot of the inventory or in
 * a slot of Curios. The menu of a terminal stays open while the item is still
 * there.
 */
public sealed interface TerminalSlot {

    StreamCodec<RegistryFriendlyByteBuf, TerminalSlot> STREAM_CODEC = TerminalSlotCodec.CODEC;

    /**
     * @return what lies in this slot of {@code player}; empty when the slot does
     *         not exist (any more)
     */
    ItemStack stackOf(Player player);

    /**
     * A hand of the player.
     */
    record Hand(InteractionHand hand) implements TerminalSlot {

        public Hand {
            Objects.requireNonNull(hand, "hand");
        }

        @Override
        public ItemStack stackOf(final Player player) {
            return player.getItemInHand(hand);
        }
    }

    /**
     * A slot of the main inventory, hotbar included; armor and the off hand are
     * not part of it.
     */
    record Carried(int index) implements TerminalSlot {

        @Override
        public ItemStack stackOf(final Player player) {
            return index >= 0 && index < Inventory.INVENTORY_SIZE ? player.getInventory().getItem(index)
                    : ItemStack.EMPTY;
        }
    }

    /**
     * A slot of Curios: the {@code index}th slot of the slot type {@code slotType}.
     */
    record Curio(String slotType, int index) implements TerminalSlot {

        public Curio {
            Objects.requireNonNull(slotType, "slotType");
        }

        @Override
        public ItemStack stackOf(final Player player) {
            return CuriosTerminals.stackAt(player, slotType, index);
        }
    }
}
