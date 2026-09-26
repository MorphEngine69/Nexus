package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.CoalGeneratorBlockEntity;
import com.morphengine.nexus.networking.CoalGeneratorViewPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Coal Generator panel: one fuel slot above the player's inventory. The server
 * sends the view every {@value #REFRESH_INTERVAL_TICKS} ticks when it changed.
 */
public final class CoalGeneratorMenu extends DeviceMenu<CoalGeneratorBlockEntity> {

    public static final int FUEL_SLOT_X = 30;
    public static final int FUEL_SLOT_Y = 46;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 112;

    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final int FUEL_SLOT = 0;
    private static final int PLAYER_SLOTS_START = 1;

    private int ticksSinceOpen;
    /** On the server the view last sent, on the client the view last received. */
    private CoalGeneratorView view = CoalGeneratorView.EMPTY;

    public CoalGeneratorMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.COAL_GENERATOR.get(), containerId, inventory, pos, CoalGeneratorBlockEntity.class);
        final CoalGeneratorBlockEntity generator = blockEntity();
        final Container fuel = viewer() != null && generator != null ? generator.fuel() : new SimpleContainer(1);
        addSlot(new FuelSlot(fuel, FUEL_SLOT_X, FUEL_SLOT_Y));
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
    }

    public CoalGeneratorView view() {
        return view;
    }

    public void acceptView(final CoalGeneratorView received) {
        view = received;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        final ServerPlayer viewer = viewer();
        final CoalGeneratorBlockEntity generator = blockEntity();
        if (viewer == null || generator == null || ticksSinceOpen++ % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        final CoalGeneratorView current = generator.view();
        if (!current.equals(view)) {
            view = current;
            PacketDistributor.sendToPlayer(viewer, new CoalGeneratorViewPayload(containerId, current));
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final boolean moved = slotIndex == FUEL_SLOT
                ? moveItemStackTo(stack, PLAYER_SLOTS_START, slots.size(), true)
                : CoalGeneratorBlockEntity.isFuel(stack)
                        && moveItemStackTo(stack, FUEL_SLOT, PLAYER_SLOTS_START, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private static final class FuelSlot extends Slot {

        FuelSlot(final Container container, final int x, final int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return CoalGeneratorBlockEntity.isFuel(stack);
        }
    }
}
