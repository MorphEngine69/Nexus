package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.MenuHost;
import com.morphengine.nexus.block.entity.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Base for the menu of a network device: a panel bound to the block entity at
 * one position, with no slots of its own. Designed for extension; subclasses
 * add the slots, what the panel shows and how it reaches the client.
 *
 * <p>The panel shows the device to whoever may open it in its network. Its
 * buttons and its own slots take {@link Permission#CONFIGURE}, or what a
 * {@link GuardedSlot} says; on the server a click or a button press the player
 * may not make does nothing. Subclasses react to buttons in {@link #pressButton}.
 *
 * @param <B> the device's block entity
 */
public abstract class DeviceMenu<B extends BlockEntity & MenuHost> extends AbstractContainerMenu
        implements DevicePanel, GuardedMenu {

    private static final Set<Permission> CONFIGURE = Set.of(Permission.CONFIGURE);

    private final DeviceBinding<B> binding;
    private final AccessSync access;
    /** The permission the device's owner lacks for it to work, as its ordinal plus one; zero while it works. */
    private final DataSlot halted = DataSlot.standalone();

    protected DeviceMenu(
            final MenuType<?> type, final int containerId, final Inventory inventory, final BlockPos pos,
            final Class<B> blockEntityType) {
        super(type, containerId);
        this.binding = new DeviceBinding<>(inventory, pos, blockEntityType);
        final Player player = inventory.player;
        this.access = player instanceof ServerPlayer
                ? AccessSync.onServer(permission -> binding.permits(player, permission)) : AccessSync.onClient();
        addDataSlot(access);
        addDataSlot(halted);
    }

    @Override
    public DeviceBinding<B> binding() {
        return binding;
    }

    public BlockPos pos() {
        return binding.pos();
    }

    /**
     * @return the device's block entity; {@code null} if it was gone when the menu opened
     */
    public @Nullable B blockEntity() {
        return binding.blockEntity();
    }

    /**
     * @return the player the panel data is sent to; {@code null} on the client
     */
    protected @Nullable ServerPlayer viewer() {
        return binding.viewer();
    }

    @Override
    public boolean stillValid(final Player player) {
        return binding.stillValid(player);
    }

    @Override
    public final boolean permits(final Player player, final Permission permission) {
        return binding.permits(player, permission);
    }

    /**
     * @return nothing for the player's inventory, what a {@link GuardedSlot}
     *         takes, {@link Permission#CONFIGURE} for any other slot of the device
     */
    @Override
    public Set<Permission> permissionsFor(final Slot slot) {
        if (slot.container instanceof Inventory) {
            return Set.of();
        }
        return slot instanceof GuardedSlot guarded ? guarded.permissions() : CONFIGURE;
    }

    /**
     * @return what the device's own slots take together, as a stack shift-clicked
     *         out of the inventory may go to any of them
     */
    @Override
    public Set<Permission> quickMovePermissions() {
        return SlotGuard.everySlot(this, this);
    }

    @Override
    public final AccessSync viewerAccess() {
        return access;
    }

    /**
     * @return the permission the device's owner lacks, so that it stands still;
     *         {@code null} while it works, or for a device that does not work on its own
     */
    public final @Nullable Permission haltedFor() {
        final int code = halted.get();
        final Permission[] permissions = Permission.values();
        return code > 0 && code <= permissions.length ? permissions[code - 1] : null;
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity() instanceof NetworkDeviceBlockEntity device) {
            final Permission missing = device.missingPermission();
            halted.set(missing != null ? missing.ordinal() + 1 : 0);
        }
        super.broadcastChanges();
    }

    @Override
    public final void clicked(
            final int slotIndex, final int buttonNum, final ClickType input, final Player player) {
        if (SlotGuard.allows(this, slotIndex, input, player)) {
            super.clicked(slotIndex, buttonNum, input, player);
        }
    }

    /**
     * Presses the button only for a player who may configure the device.
     */
    @Override
    public final boolean clickMenuButton(final Player player, final int buttonId) {
        return permits(player, Permission.CONFIGURE) && pressButton(player, buttonId);
    }

    /**
     * What button {@code buttonId} of the panel does, for a player allowed to
     * press it. Server side.
     *
     * @return whether the button exists and did something
     */
    protected boolean pressButton(final Player player, final int buttonId) {
        return false;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        return ItemStack.EMPTY;
    }

    /**
     * Shift-click for a panel whose own slots come first, followed by the
     * player's inventory: a stack from the panel goes to the inventory, a stack
     * from the inventory to the panel slots in the given range, if any.
     *
     * @param panelSlots how many slots the panel has, before the inventory
     * @param targetStart first panel slot the clicked stack may go to
     * @param targetEnd   end, exclusive, of those slots; equal to {@code targetStart} if none
     */
    protected final ItemStack shiftClick(
            final int slotIndex, final int panelSlots, final int targetStart, final int targetEnd) {
        final Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        final boolean moved = slotIndex < panelSlots
                ? moveItemStackTo(stack, panelSlots, slots.size(), true)
                : targetStart < targetEnd && moveItemStackTo(stack, targetStart, targetEnd, false);
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

    @Override
    public void removed(final Player player) {
        super.removed(player);
        binding.release();
    }
}
