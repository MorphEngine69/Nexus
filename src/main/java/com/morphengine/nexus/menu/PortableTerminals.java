package com.morphengine.nexus.menu;

import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Opening the menu of a Nexus Terminal, on the server as the player uses it
 * and on the client from what the server sends: the hand it is in, then its
 * settings.
 */
public final class PortableTerminals {

    private PortableTerminals() {
    }

    /**
     * Opens the Nexus Terminal in {@code hand} as its mode says. Server side only.
     */
    public static void open(final ServerPlayer player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final TerminalKind mode = NexusTerminalItem.modeOf(stack);
        final TerminalSettings settings = settingsOf(stack);
        final TerminalOpening opening = new TerminalOpening(
                new HandTerminalBinding(player, hand, new PortableTerminal(player, hand, mode)), settings);
        player.openMenu(new SimpleMenuProvider((containerId, inventory, opener) -> create(mode, containerId,
                inventory, opening), stack.getHoverName()), buffer -> {
                    buffer.writeEnum(hand);
                    TerminalSettings.STREAM_CODEC.encode(buffer, settings);
                });
    }

    /**
     * @return what the client's menu of a Nexus Terminal opens with, from what {@link #open} sent
     */
    public static TerminalOpening readOpening(final Inventory inventory, final RegistryFriendlyByteBuf buffer) {
        final InteractionHand hand = buffer.readEnum(InteractionHand.class);
        return new TerminalOpening(new HandTerminalBinding(inventory.player, hand, null),
                TerminalSettings.STREAM_CODEC.decode(buffer));
    }

    private static TerminalSettings settingsOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.TERMINAL_SETTINGS.get(), TerminalSettings.DEFAULT);
    }

    private static AbstractContainerMenu create(
            final TerminalKind mode, final int containerId, final Inventory inventory, final TerminalOpening opening) {
        return switch (mode) {
            case TERMINAL -> new TerminalMenu(NexusMenuTypes.PORTABLE_TERMINAL.get(), containerId, inventory,
                    opening);
            case CRAFTING_TERMINAL -> new CraftingTerminalMenu(NexusMenuTypes.PORTABLE_CRAFTING_TERMINAL.get(),
                    containerId, inventory, opening);
            case BLUEPRINT_TERMINAL -> new BlueprintTerminalMenu(NexusMenuTypes.PORTABLE_BLUEPRINT_TERMINAL.get(),
                    containerId, inventory, opening);
        };
    }
}
