package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.block.entity.DeviceUpgrades;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.energy.EnergyRateMeter;
import com.morphengine.nexus.networking.EnergyCellViewPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Panel of one Energy Cell: its own charge and throughput, in the color of its
 * network, its priority in the network's pool with buttons to change it, the
 * upgrade slots and the player's inventory.
 * The server refreshes the view every {@value #REFRESH_INTERVAL_TICKS} ticks
 * and sends it only when it changed; the priority returns in a data slot.
 */
public final class EnergyCellMenu extends DeviceMenu<EnergyCellBlockEntity> {

    /** Menu button id of the first {@link PriorityButtons priority button}. */
    public static final int BUTTON_PRIORITY = 0;
    /** Menu button id that moves the first side, DOWN, to its next mode; the other five sides follow it. */
    public static final int BUTTON_SIDE_NEXT = 8;
    /** Menu button id that moves the first side, DOWN, to its previous mode; the other five sides follow it. */
    public static final int BUTTON_SIDE_PREVIOUS = 16;

    public static final int UPGRADES_LEFT = 208;
    public static final int UPGRADES_TOP = 52;
    public static final int INVENTORY_LEFT = 37;
    public static final int INVENTORY_TOP = 136;

    private static final int REFRESH_INTERVAL_TICKS = 20;

    private final EnergyRateMeter meter = new EnergyRateMeter();
    private final DataSlot priority = DataSlot.standalone();
    private final DataSlot sides = DataSlot.standalone();
    private int ticksSinceOpen;
    /** On the server the view last sent, on the client the view last received. */
    private EnergyCellView view = EnergyCellView.EMPTY;

    public EnergyCellMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.ENERGY_CELL.get(), containerId, inventory, pos, EnergyCellBlockEntity.class);
        final EnergyCellBlockEntity cell = blockEntity();
        UpgradeColumn.slots(viewer() != null && cell != null ? cell.upgrades() : null, DeviceUpgrades.LIMITS,
                UPGRADES_LEFT, UPGRADES_TOP)
                .forEach(this::addSlot);
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(priority);
        addDataSlot(sides);
    }

    public int priority() {
        return priority.get();
    }

    /**
     * @return what the panel shows of the sides of the cell: the modes the server last sent
     */
    public SideConfig<Direction> sideModes() {
        return SideConfig.fromBits(Direction.class, sides.get());
    }

    @Override
    protected boolean pressButton(final Player player, final int buttonId) {
        final EnergyCellBlockEntity cell = blockEntity();
        if (cell == null) {
            return false;
        }
        final int index = buttonId - BUTTON_PRIORITY;
        if (index >= 0 && index < PriorityButtons.count()) {
            cell.setEnergyPriority(cell.energyPriority() + PriorityButtons.stepOf(index));
            return true;
        }
        return moveSide(cell, buttonId - BUTTON_SIDE_NEXT, true)
                || moveSide(cell, buttonId - BUTTON_SIDE_PREVIOUS, false);
    }

    private static boolean moveSide(final EnergyCellBlockEntity cell, final int index, final boolean forward) {
        if (index < 0 || index >= Direction.values().length) {
            return false;
        }
        final Direction side = Direction.values()[index];
        final SideMode mode = cell.sideMode(side);
        cell.setSideMode(side, forward ? mode.next() : mode.previous());
        return true;
    }

    public EnergyCellView view() {
        return view;
    }

    public void acceptView(final EnergyCellView received) {
        view = received;
    }

    @Override
    public void broadcastChanges() {
        final ServerPlayer viewer = viewer();
        final EnergyCellBlockEntity cell = blockEntity();
        if (viewer != null && cell != null) {
            priority.set(cell.energyPriority());
            sides.set(cell.sideBits());
        }
        super.broadcastChanges();
        if (viewer == null || cell == null || ticksSinceOpen++ % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        final EnergyBuffer buffer = cell.energyBuffer();
        meter.sample(buffer, REFRESH_INTERVAL_TICKS);
        final EnergyCellView current = new EnergyCellView(buffer.stored(), buffer.capacity(),
                meter.inputPerTick(), meter.outputPerTick(), cell.networkBadge());
        if (!current.equals(view)) {
            view = current;
            PacketDistributor.sendToPlayer(viewer, new EnergyCellViewPayload(containerId, current));
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final boolean isUpgrade = UpgradeColumn.takes(DeviceUpgrades.LIMITS, slots.get(slotIndex).getItem());
        return shiftClick(slotIndex, DeviceUpgrades.SIZE, 0, isUpgrade ? DeviceUpgrades.SIZE : 0);
    }
}
