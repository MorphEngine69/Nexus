package com.morphengine.nexus.terminal;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.level.PlayerActor;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * What a click on a terminal's grid does to the network's storage, the item the
 * player carries and their inventory. A fluid container on the cursor is filled
 * from a fluid clicked in the grid, or emptied into the network when clicked
 * anywhere else; everything else carried is stored as an item. Server thread only.
 */
public final class TerminalActions {

    private final ServerPlayer player;
    private final AbstractContainerMenu menu;
    private final Storage storage;
    private final Actor actor;
    private final Runnable onTake;

    /**
     * @param onTake told each time the player took something out of the network, so that it can be paid for
     */
    public TerminalActions(
            final ServerPlayer player, final AbstractContainerMenu menu, final Storage storage,
            final Runnable onTake) {
        this.player = player;
        this.menu = menu;
        this.storage = storage;
        this.onTake = onTake;
        this.actor = PlayerActor.of(player);
    }

    /**
     * @param resource the resource under the cursor; {@code null} for an empty spot of the grid
     */
    public void click(final @Nullable NexusResource resource, final GridClick click) {
        if (click == GridClick.QUICK_MOVE) {
            if (resource instanceof ItemKey item) {
                moveToInventory(item);
            }
            return;
        }
        if (menu.getCarried().isEmpty()) {
            if (resource instanceof ItemKey item) {
                takeToCursor(item, click == GridClick.SECONDARY);
            }
            return;
        }
        if (resource instanceof FluidKey fluid && fillCarried(fluid) || emptyCarried()) {
            return;
        }
        insertCarried(click == GridClick.PRIMARY);
    }

    /**
     * Stores as much of {@code stack} as fits and shrinks it by that much.
     */
    public void insert(final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        final long inserted = storage.insert(ItemKey.of(stack), stack.getCount(), Action.EXECUTE, actor);
        stack.shrink((int) inserted);
    }

    private void takeToCursor(final ItemKey item, final boolean half) {
        final long available = storage.extract(item, item.maxStackSize(), Action.SIMULATE, actor);
        final long wanted = half ? (available + 1) / 2 : available;
        if (wanted <= 0) {
            return;
        }
        final long extracted = storage.extract(item, wanted, Action.EXECUTE, actor);
        menu.setCarried(item.toStack((int) extracted));
        onTake.run();
    }

    private void moveToInventory(final ItemKey item) {
        final int room = Math.min(roomInInventory(item), item.maxStackSize());
        if (room <= 0) {
            return;
        }
        final long extracted = storage.extract(item, room, Action.EXECUTE, actor);
        if (extracted > 0) {
            onTake.run();
            final ItemStack stack = item.toStack((int) extracted);
            player.getInventory().add(stack);
            insert(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }

    private int roomInInventory(final ItemKey item) {
        final ItemStack template = item.toStack(1);
        int room = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                room += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(stack, template)) {
                room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
        }
        return Math.min(room, Inventory.INVENTORY_SIZE * template.getMaxStackSize());
    }

    private void insertCarried(final boolean everything) {
        final ItemStack carried = menu.getCarried();
        final int offered = everything ? carried.getCount() : 1;
        final long inserted = storage.insert(ItemKey.of(carried), offered, Action.EXECUTE, actor);
        if (inserted > 0) {
            carried.shrink((int) inserted);
            menu.setCarried(carried);
        }
    }

    private boolean fillCarried(final FluidKey fluid) {
        final ResourceHandler<FluidResource> container = carriedFluidHandler();
        final long available = container != null
                ? storage.extract(fluid, Integer.MAX_VALUE, Action.SIMULATE, actor) : 0;
        if (container == null || available <= 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int accepted = container.insert(fluid.fluid(), (int) available, transaction);
            if (accepted <= 0) {
                return false;
            }
            final long extracted = storage.extract(fluid, accepted, Action.EXECUTE, actor);
            if (extracted != accepted) {
                giveBack(fluid, extracted);
                return false;
            }
            transaction.commit();
            onTake.run();
            return true;
        }
    }

    private boolean emptyCarried() {
        final ResourceHandler<FluidResource> container = carriedFluidHandler();
        if (container == null) {
            return false;
        }
        for (int index = 0; index < container.size(); index++) {
            final FluidResource fluid = container.getResource(index);
            if (!fluid.isEmpty() && pour(container, index, new FluidKey(fluid))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pours what the network can take from one tank of the carried container;
     * containers that only empty whole, like buckets, pour nothing unless all fits.
     */
    private boolean pour(final ResourceHandler<FluidResource> container, final int index, final FluidKey fluid) {
        final long held = container.getAmountAsLong(index);
        final long room = storage.insert(fluid, held, Action.SIMULATE, actor);
        if (room <= 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int drained = container.extract(index, fluid.fluid(), (int) Math.min(room, Integer.MAX_VALUE),
                    transaction);
            if (drained <= 0) {
                return false;
            }
            final long inserted = storage.insert(fluid, drained, Action.EXECUTE, actor);
            if (inserted != drained) {
                takeBack(fluid, inserted);
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    /**
     * Undoes an extract whose transaction is about to roll back.
     */
    private void giveBack(final FluidKey fluid, final long amount) {
        if (amount > 0) {
            storage.insert(fluid, amount, Action.EXECUTE, actor);
        }
    }

    /**
     * Undoes an insert whose transaction is about to roll back.
     */
    private void takeBack(final FluidKey fluid, final long amount) {
        if (amount > 0) {
            storage.extract(fluid, amount, Action.EXECUTE, actor);
        }
    }

    private @Nullable ResourceHandler<FluidResource> carriedFluidHandler() {
        return ItemAccess.forPlayerCursor(player, menu).getCapability(Capabilities.Fluid.ITEM);
    }
}
