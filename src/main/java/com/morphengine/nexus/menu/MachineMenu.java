package com.morphengine.nexus.menu;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.machine.InputMode;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.machine.MachineTier;
import com.morphengine.nexus.networking.MachineViewPayload;
import com.morphengine.nexus.processing.ItemStackSlots;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachineOutput;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transfer.ItemResource;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Panel of a machine: a column for every line, its input slot over a bar of its progress over its output slot,
 * the slot of the Speed Upgrades, the player's inventory, a button for the input mode and the choice of the working
 * sides. The server refreshes the view every {@value #REFRESH_INTERVAL_TICKS} ticks and sends it only when it
 * changed; the settings return in a data slot.
 */
public final class MachineMenu extends DeviceMenu<MachineBlockEntity> {

    /** Menu button id that switches the input mode. */
    public static final int BUTTON_MODE = 0;
    /** Menu button id that moves the redstone mode to the next one. */
    public static final int BUTTON_REDSTONE = 1;
    /** Menu button id that moves the first side, TOP, to its next mode; the other five sides follow it. */
    public static final int BUTTON_SIDE_NEXT = 8;
    /** Menu button id that moves the first side, TOP, to its previous mode; the other five sides follow it. */
    public static final int BUTTON_SIDE_PREVIOUS = 16;

    public static final int IMAGE_WIDTH = 206;
    public static final int LINES_LEFT = 10;
    public static final int LINES_TOP = 70;
    public static final int LINE_PITCH = 22;
    public static final int SLOT_SIZE = 18;
    /** From the top of the input slot of a line to the top of its output slot. */
    public static final int OUTPUT_OFFSET = 54;
    public static final int UPGRADE_LEFT = 178;
    public static final int UPGRADE_TOP = LINES_TOP;
    public static final int INVENTORY_LEFT = 22;
    public static final int INVENTORY_TOP = LINES_TOP + MachineBlockEntity.UPGRADE_SLOTS * SLOT_SIZE + 24;
    public static final int IMAGE_HEIGHT = INVENTORY_TOP + 84;

    private static final int REFRESH_INTERVAL_TICKS = 1;
    private static final int MODE_BITS_SHIFT = 12;
    private static final int REDSTONE_BITS_SHIFT = 13;
    private static final int REDSTONE_BITS_MASK = 0b11;

    private final int pairs;
    private final int inputsPerLine;
    private final boolean hasTank;
    private final DataSlot settings = DataSlot.standalone();
    private int ticksSinceOpen;
    /** On the server the view last sent, on the client the view last received. */
    private MachineView view = MachineView.EMPTY;

    public MachineMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.MACHINE.get(), containerId, inventory, pos, MachineBlockEntity.class);
        final MachineKind kind = kindAt(inventory.player.level(), pos);
        this.pairs = kind.shape().linesOf(tierAt(inventory.player.level(), pos));
        this.inputsPerLine = kind.shape().inputsPerLine();
        this.hasTank = kind.output() == MachineOutput.FLUID;
        final MachineBlockEntity machine = blockEntity();
        final boolean onServer = viewer() != null && machine != null;
        final Container inputs = onServer ? machine.slots().inputs() : new SimpleContainer(ItemStackSlots.MAX_LINES);
        final Container outputs = onServer ? machine.slots().outputs() : new SimpleContainer(ItemStackSlots.MAX_LINES);
        for (int slot = 0; slot < inputCount(); slot++) {
            addSlot(new InputSlot(inputs, slot, columnX(slot, inputCount()), LINES_TOP, onServer ? machine : null));
        }
        for (int line = 0; line < outputSlotCount(); line++) {
            addSlot(new OutputSlot(outputs, line, outputX(line, inputsPerLine, inputCount()),
                    LINES_TOP + OUTPUT_OFFSET));
        }
        final Container upgrades = onServer ? machine.upgrades() : new UpgradeContainer(
                MachineBlockEntity.UPGRADE_SLOTS, MachineBlockEntity.UPGRADE_LIMITS, () -> { });
        for (int slot = 0; slot < MachineBlockEntity.UPGRADE_SLOTS; slot++) {
            addSlot(new UpgradeSlot(upgrades, slot, UPGRADE_LEFT, UPGRADE_TOP + slot * SLOT_SIZE));
        }
        InventorySlots.add(this::addSlot, inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlot(settings);
    }

    /**
     * @return the kind of the machine at {@code pos}; a furnace when there is no machine
     */
    private static MachineKind kindAt(final Level level, final BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof MachineBlock block
                ? block.kind() : MachineKind.ENERGY_FURNACE;
    }

    private static MachineTier tierAt(final Level level, final BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof MachineBlock block ? block.tier() : MachineTier.BASIC;
    }

    /**
     * The input slots stand in one row, centred in the room that seven columns of slots take.
     */
    public static int columnX(final int column, final int columns) {
        return LINES_LEFT + (ItemStackSlots.MAX_LINES - columns) * LINE_PITCH / 2 + column * LINE_PITCH;
    }

    /**
     * @return where the output of line {@code line} stands: under the middle input of the line
     */
    public static int outputX(final int line, final int inputsPerLine, final int columns) {
        return columnX(line * inputsPerLine + inputsPerLine / 2, columns);
    }

    public int inputCount() {
        return pairs * inputsPerLine;
    }

    /**
     * @return output slots in the panel: one for each line, none for a machine whose output is a tank
     */
    public int outputSlotCount() {
        return hasTank ? 0 : pairs;
    }

    public int inputsPerLine() {
        return inputsPerLine;
    }

    public boolean hasTank() {
        return hasTank;
    }

    /**
     * @return whether the player chooses how the input is shared out: only where a line has an input slot to itself
     */
    public boolean hasInputMode() {
        return pairs > 1 && inputsPerLine == 1;
    }

    public int pairs() {
        return pairs;
    }

    /**
     * @return what the panel shows of the sides of the machine: the modes the server last sent
     */
    public SideConfig<MachineSide> sideModes() {
        return SideConfig.fromBits(MachineSide.class, settings.get() & (1 << MODE_BITS_SHIFT) - 1);
    }

    public InputMode inputMode() {
        return InputMode.values()[settings.get() >> MODE_BITS_SHIFT & 1];
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.values()[settings.get() >> REDSTONE_BITS_SHIFT & REDSTONE_BITS_MASK];
    }

    public MachineView view() {
        return view;
    }

    public void acceptView(final MachineView received) {
        view = received;
    }

    @Override
    protected boolean pressButton(final Player player, final int buttonId) {
        final MachineBlockEntity machine = blockEntity();
        if (machine == null) {
            return false;
        }
        if (buttonId == BUTTON_REDSTONE) {
            final RedstoneMode[] modes = RedstoneMode.values();
            machine.redstone().changeMode(modes[(machine.redstone().mode().ordinal() + 1) % modes.length]);
            return true;
        }
        if (buttonId == BUTTON_MODE) {
            final InputMode[] modes = InputMode.values();
            machine.machine().inventory().setMode(
                    modes[(machine.machine().inventory().mode().ordinal() + 1) % modes.length]);
            machine.setChanged();
            return true;
        }
        return moveSide(machine, buttonId - BUTTON_SIDE_NEXT, true)
                || moveSide(machine, buttonId - BUTTON_SIDE_PREVIOUS, false);
    }

    private static boolean moveSide(final MachineBlockEntity machine, final int index, final boolean forward) {
        if (index < 0 || index >= MachineSide.values().length) {
            return false;
        }
        final MachineSide side = MachineSide.values()[index];
        final SideMode mode = machine.machine().sides().mode(side);
        machine.setSideMode(side, forward ? mode.next() : mode.previous());
        return true;
    }

    @Override
    public void broadcastChanges() {
        final ServerPlayer viewer = viewer();
        final MachineBlockEntity machine = blockEntity();
        if (viewer != null && machine != null) {
            settings.set(machine.machine().sides().toBits()
                    | machine.machine().inventory().mode().ordinal() << MODE_BITS_SHIFT
                    | machine.redstone().mode().ordinal() << REDSTONE_BITS_SHIFT);
        }
        super.broadcastChanges();
        if (viewer == null || machine == null || ticksSinceOpen++ % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        final MachineView current = machine.view();
        if (!current.equals(view)) {
            view = current;
            PacketDistributor.sendToPlayer(viewer, new MachineViewPayload(containerId, current));
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
        final int panelSlots = inputCount() + outputSlotCount() + MachineBlockEntity.UPGRADE_SLOTS;
        final boolean moved = slotIndex < panelSlots
                ? moveItemStackTo(stack, panelSlots, slots.size(), true)
                : moveIntoMachine(stack);
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

    /**
     * A speed upgrade goes to its slot. Anything else fills the input slots one after another, as the player asked by
     * hand, when each resource has a slot of its own; when one resource is spread over the lines, the machine
     * shares it out, so on the client, where the machine does not hold the slots, that is only guessed at.
     */
    private boolean moveIntoMachine(final ItemStack stack) {
        final int upgradeSlot = inputCount() + outputSlotCount();
        if (MachineBlockEntity.UPGRADE_LIMITS.takesKindOf(stack)) {
            return moveItemStackTo(stack, upgradeSlot, upgradeSlot + MachineBlockEntity.UPGRADE_SLOTS, false);
        }
        final MachineBlockEntity machine = blockEntity();
        if (viewer() == null || machine == null || !hasInputMode() || inputMode() == InputMode.PER_RESOURCE) {
            return moveItemStackTo(stack, 0, inputCount(), false);
        }
        if (!machine.accepts(ItemResource.of(stack))) {
            return false;
        }
        final long accepted = machine.machine().inventory().insert(ItemKey.of(stack), stack.getCount(),
                Action.EXECUTE, Actor.NOBODY);
        stack.shrink((int) accepted);
        return accepted > 0;
    }

    /** An input slot, which takes what the machine has a recipe for. */
    private static final class InputSlot extends Slot {

        private final @Nullable MachineBlockEntity machine;

        InputSlot(final Container container, final int index, final int x, final int y,
                  final @Nullable MachineBlockEntity machine) {
            super(container, index, x, y);
            this.machine = machine;
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return machine == null || machine.accepts(ItemResource.of(stack));
        }
    }

    /** An output slot, which gives out what the machine made and takes nothing. */
    private static final class OutputSlot extends Slot {

        OutputSlot(final Container container, final int index, final int x, final int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }
    }
}
