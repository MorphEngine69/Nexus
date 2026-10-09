package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.energy.DeviceEnergyMeter;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.menu.BlockTerminalBinding;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.TerminalHost;
import com.morphengine.nexus.menu.TerminalMenu;
import com.morphengine.nexus.menu.TerminalOpening;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.terminal.TerminalKind;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * A terminal: a window onto its network's storage. It keeps how the player
 * likes the list sorted and sized; a crafting terminal keeps its crafting
 * grid, a blueprint terminal its encoder. Once a second it lights or darkens
 * its screen with the network's energy.
 */
public final class TerminalBlockEntity extends AnimatedDeviceBlockEntity implements TerminalHost {

    private static final RawAnimation AT_REST = RawAnimation.begin().thenLoop("idle");
    private static final String TAG_SETTINGS = "settings";
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private final TerminalKind kind;
    private final @Nullable TerminalCraftingGrid craftingGrid;
    private final @Nullable BlueprintEncoder encoder;
    private TerminalSettings settings = TerminalSettings.DEFAULT;

    public TerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.TERMINAL.get(), pos, state, current -> AT_REST);
        this.kind = kindOf(state);
        this.craftingGrid = kind.hasCraftingGrid() ? new TerminalCraftingGrid(this::setChanged) : null;
        this.encoder = kind.hasEncoder() ? new BlueprintEncoder(this::setChanged) : null;
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

    @Override
    public void changeSettings(final TerminalSettings newSettings) {
        settings = newSettings;
        setChanged();
    }

    /**
     * @return the crafting grid; {@code null} for a terminal without one
     */
    @Override
    public @Nullable TerminalCraftingGrid craftingGrid() {
        return craftingGrid;
    }

    /**
     * @return the Blueprint encoder; {@code null} for a terminal without one
     */
    @Override
    public @Nullable BlueprintEncoder encoder() {
        return encoder;
    }

    /**
     * @return the network's autocrafting while the terminal is {@link TerminalStatus#ONLINE};
     *         {@code null} otherwise. Server side only.
     */
    @Override
    public @Nullable AutocraftingComponent onlineAutocrafting() {
        final NetworkController controller = controller();
        if (controller == null || status() != TerminalStatus.ONLINE) {
            return null;
        }
        return controller.component(NetworkComponentTypes.AUTOCRAFTING);
    }

    @Override
    public DeviceEnergyMeter tollPayer(final NetworkController network) {
        return energyMeter();
    }

    @Override
    public @Nullable NetworkController onlineNetwork() {
        return status() == TerminalStatus.ONLINE ? controller() : null;
    }

    @Override
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
    @Override
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
    @Override
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
        final TerminalOpening opening = new TerminalOpening(new BlockTerminalBinding(inventory, worldPosition),
                settings);
        return switch (kind) {
            case TERMINAL -> new TerminalMenu(NexusMenuTypes.TERMINAL.get(), containerId, inventory, opening);
            case CRAFTING_TERMINAL -> new CraftingTerminalMenu(NexusMenuTypes.CRAFTING_TERMINAL.get(), containerId,
                    inventory, opening);
            case BLUEPRINT_TERMINAL -> new BlueprintTerminalMenu(NexusMenuTypes.BLUEPRINT_TERMINAL.get(),
                    containerId, inventory, opening);
        };
    }

    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && craftingGrid != null) {
            Containers.dropContents(level, pos, craftingGrid);
        }
        if (level != null && encoder != null) {
            Containers.dropContents(level, pos, encoder.blueprints());
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueOutput output = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        output.store(TAG_SETTINGS, TerminalSettings.CODEC, settings);
        if (craftingGrid != null) {
            output.saveItems(craftingGrid.stacks());
        }
        if (encoder != null) {
            encoder.write(output);
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueInput input = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        settings = input.read(TAG_SETTINGS, TerminalSettings.CODEC).orElse(TerminalSettings.DEFAULT);
        if (craftingGrid != null) {
            input.loadItems(craftingGrid.stacks());
        }
        if (encoder != null) {
            encoder.read(input);
        }
    }
}
