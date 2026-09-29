package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.TerminalMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Terminal or Crafting Terminal: a window onto its network's storage. It
 * keeps how the player likes the list sorted and sized, and a crafting terminal
 * keeps its crafting grid. Once a second it lights or darkens its screen with
 * the network's energy.
 */
public final class TerminalBlockEntity extends NetworkDeviceBlockEntity {

    private static final String TAG_SETTINGS = "settings";
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private final TerminalKind kind;
    private final @Nullable TerminalCraftingGrid craftingGrid;
    private TerminalSettings settings = TerminalSettings.DEFAULT;

    public TerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.TERMINAL.get(), pos, state);
        this.kind = kindOf(state);
        this.craftingGrid = kind.hasCraftingGrid() ? new TerminalCraftingGrid(this::setChanged) : null;
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state, final TerminalBlockEntity terminal) {
        if (level.getGameTime() % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        final boolean powered = terminal.status() == TerminalStatus.ONLINE;
        if (state.getValue(TerminalBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(TerminalBlock.POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    private static TerminalKind kindOf(final BlockState state) {
        if (state.getBlock() instanceof TerminalBlock terminal) {
            return terminal.kind();
        }
        throw new IllegalStateException("terminal block entity on a non terminal block: " + state);
    }

    public TerminalKind kind() {
        return kind;
    }

    public TerminalSettings settings() {
        return settings;
    }

    public void changeSettings(final TerminalSettings newSettings) {
        settings = newSettings;
        setChanged();
    }

    /**
     * @return the crafting grid; {@code null} for a terminal without one
     */
    public @Nullable TerminalCraftingGrid craftingGrid() {
        return craftingGrid;
    }

    public TerminalStatus status() {
        if (controller() == null) {
            return TerminalStatus.NO_NETWORK;
        }
        return isNetworkPowered() ? TerminalStatus.ONLINE : TerminalStatus.NO_ENERGY;
    }

    /**
     * @return everything the network holds, its energy pool included, while the
     *         terminal is {@link TerminalStatus#ONLINE}; {@code null} otherwise.
     *         Server side only.
     */
    public @Nullable Storage onlineResources() {
        final NetworkController controller = controller();
        if (controller == null || status() != TerminalStatus.ONLINE) {
            return null;
        }
        return controller.resources();
    }

    /**
     * @return the storage of the network while the terminal is {@link TerminalStatus#ONLINE};
     *         {@code null} otherwise. Server side only.
     */
    public @Nullable NetworkStorage onlineStorage() {
        final NetworkController controller = controller();
        if (controller == null || status() != TerminalStatus.ONLINE) {
            return null;
        }
        return controller.component(NetworkComponentTypes.STORAGE).storage();
    }

    @Override
    public void writeMenuData(final RegistryFriendlyByteBuf buffer) {
        TerminalSettings.STREAM_CODEC.encode(buffer, settings);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return switch (kind) {
            case TERMINAL -> new TerminalMenu(containerId, inventory, worldPosition, settings);
            case CRAFTING_TERMINAL -> new CraftingTerminalMenu(containerId, inventory, worldPosition, settings);
        };
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && craftingGrid != null) {
            Containers.dropContents(level, pos, craftingGrid);
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_SETTINGS, TerminalSettings.CODEC, settings);
        if (craftingGrid != null) {
            ContainerHelper.saveAllItems(output, craftingGrid.stacks());
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        settings = input.read(TAG_SETTINGS, TerminalSettings.CODEC).orElse(TerminalSettings.DEFAULT);
        if (craftingGrid != null) {
            ContainerHelper.loadAllItems(input, craftingGrid.stacks());
        }
    }
}
