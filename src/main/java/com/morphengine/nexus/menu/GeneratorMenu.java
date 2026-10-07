package com.morphengine.nexus.menu;

import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.networking.GeneratorViewPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.transport.SideConfig;
import com.morphengine.nexus.transport.SideMode;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Predicate;

/**
 * Generator panel: the input slot, which takes fuel or a bucket of the fluid the generator burns, and the upgrade
 * slots above the player's inventory. A bucket put in the slot pours into the tanks and the empty bucket stays for the
 * player to take. The server sends the view every {@value #REFRESH_INTERVAL_TICKS} ticks when it changed.
 */
public final class GeneratorMenu extends DeviceMenu<GeneratorBlockEntity> {

    public static final int INPUT_SLOT_X = 19;
    /** The input slot, the gauges and the charge bar are centred on one line, the same as the column of upgrades. */
    public static final int INPUT_SLOT_Y = 53;
    /** The four upgrade slots stand one under another on the right, as far above the centre line as below it. */
    public static final int UPGRADE_SLOT_Y = 26;
    public static final int UPGRADE_SLOT_X = 173;
    public static final int UPGRADE_PITCH = 18;
    public static final int INVENTORY_LEFT = 19;
    public static final int INVENTORY_TOP = 108;

    public static final int BUTTON_SIDE_NEXT = MachineMenu.BUTTON_SIDE_NEXT;
    public static final int BUTTON_SIDE_PREVIOUS = MachineMenu.BUTTON_SIDE_PREVIOUS;

    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final int INPUT_SLOT = 0;
    private static final int UPGRADE_SLOT = 1;
    private static final int PLAYER_SLOTS_START = 1 + GeneratorBlockEntity.UPGRADE_SLOTS;

    private final DataSlot settings = DataSlot.standalone();
    private int ticksSinceOpen;
    /** On the server the view last sent, on the client the view last received. */
    private GeneratorView view = GeneratorView.EMPTY;

    public GeneratorMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.GENERATOR.get(), containerId, inventory, pos, GeneratorBlockEntity.class);
        final GeneratorBlockEntity generator = blockEntity();
        final boolean onServer = viewer() != null && generator != null;
        final Container input = onServer ? generator.input() : new SimpleContainer(1);
        final Container upgrades = onServer ? generator.upgrades() : new UpgradeContainer(
                GeneratorBlockEntity.UPGRADE_SLOTS, GeneratorBlockEntity.UPGRADE_LIMITS, () -> { });
        final Predicate<ItemStack> accepts = generator != null ? generator.kind()::takesIn : stack -> false;
        final boolean bucket = generator != null && generator.kind().burnsFluid();
        addSlot(new InputSlot(input, accepts, bucket));
        for (int slot = 0; slot < GeneratorBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADE_SLOT_X,
                    UPGRADE_SLOT_Y + slot * UPGRADE_PITCH));
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(settings);
    }

    public GeneratorView view() {
        return view;
    }

    /**
     * @return what the panel shows of the sides of the generator: the modes the server last sent
     */
    public SideConfig<MachineSide> sideModes() {
        return SideConfig.fromBits(MachineSide.class, settings.get());
    }

    public void acceptView(final GeneratorView received) {
        view = received;
    }

    @Override
    protected boolean pressButton(final Player player, final int buttonId) {
        final GeneratorBlockEntity generator = blockEntity();
        return generator != null && (moveSide(generator, buttonId - BUTTON_SIDE_NEXT, true)
                || moveSide(generator, buttonId - BUTTON_SIDE_PREVIOUS, false));
    }

    private static boolean moveSide(final GeneratorBlockEntity generator, final int index, final boolean forward) {
        if (index < 0 || index >= MachineSide.values().length) {
            return false;
        }
        final MachineSide side = MachineSide.values()[index];
        final SideMode mode = generator.sides().mode(side);
        generator.setSideMode(side, forward ? mode.next() : mode.previous());
        return true;
    }

    @Override
    public void broadcastChanges() {
        final ServerPlayer viewer = viewer();
        final GeneratorBlockEntity generator = blockEntity();
        if (viewer != null && generator != null) {
            settings.set(generator.sides().toBits());
        }
        super.broadcastChanges();
        if (viewer == null || generator == null || ticksSinceOpen++ % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        final GeneratorView current = generator.view();
        if (!current.equals(view)) {
            view = current;
            PacketDistributor.sendToPlayer(viewer, new GeneratorViewPayload(containerId, current));
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
        final boolean moved = slotIndex < PLAYER_SLOTS_START
                ? moveItemStackTo(stack, PLAYER_SLOTS_START, slots.size(), true)
                : moveIntoGenerator(stack);
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

    private boolean moveIntoGenerator(final ItemStack stack) {
        if (slots.get(INPUT_SLOT).mayPlace(stack)) {
            return moveItemStackTo(stack, INPUT_SLOT, UPGRADE_SLOT, false);
        }
        return slots.get(UPGRADE_SLOT).mayPlace(stack)
                && moveItemStackTo(stack, UPGRADE_SLOT, PLAYER_SLOTS_START, false);
    }

    /** The slot for fuel, or for a bucket, which holds one container at a time. */
    private static final class InputSlot extends Slot {

        private final Predicate<ItemStack> accepts;
        private final boolean bucket;

        InputSlot(final Container container, final Predicate<ItemStack> accepts, final boolean bucket) {
            super(container, 0, INPUT_SLOT_X, INPUT_SLOT_Y);
            this.accepts = accepts;
            this.bucket = bucket;
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return accepts.test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return bucket ? 1 : super.getMaxStackSize();
        }
    }
}
