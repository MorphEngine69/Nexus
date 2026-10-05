package com.morphengine.nexus.block.entity;

import com.geckolib.animation.RawAnimation;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.Network;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.level.NetworkComponent;
import com.morphengine.nexus.level.NetworkComponentType;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkGuard;
import com.morphengine.nexus.level.NetworkState;
import com.morphengine.nexus.menu.NexusMenu;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Nexus: leads the network it is in. Its network keeps an id for good, so a
 * Nexus Terminal still finds the network after it is renamed, and after the
 * Nexus is broken and placed elsewhere: the dropped Nexus carries the id, name
 * and color of its network.
 */
public final class NexusBlockEntity extends AnimatedBlockEntity
        implements NetworkController, MenuHost, Renamable {

    private static final String TAG_NETWORK_ID = "network_id";
    private static final String TAG_NETWORK_NAME = "network_name";
    private static final String TAG_NETWORK_COLOR = "network_color";
    private static final String TAG_ESTABLISHED_AT = "established_at";
    /** Not established yet: treated as the newest, so it never outranks a running Nexus. */
    private static final long NOT_ESTABLISHED = Long.MAX_VALUE;

    private static final Map<NexusStatus, RawAnimation> ANIMATIONS = new EnumMap<>(Map.of(
            NexusStatus.ONLINE, RawAnimation.begin().thenLoop("online"),
            NexusStatus.NO_ENERGY, RawAnimation.begin().thenLoop("idle"),
            NexusStatus.CONFLICT, RawAnimation.begin().thenLoop("conflict")));

    private final NetworkState networkState = new NetworkState(this);
    private final ClickGuard clickGuard = new ClickGuard();
    private final DeviceUpgrades upgrades = new DeviceUpgrades(this);
    private final NetworkGuard guard = new NetworkGuard(this::establish);
    private Network network = NexusNetworks.createDefault();
    private long establishedAt = NOT_ESTABLISHED;

    public NexusBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.NEXUS.get(), pos, state,
                current -> ANIMATIONS.get(current.getValue(NexusBlock.STATUS)));
    }

    @Override
    public Network network() {
        return network;
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState blockState, final NexusBlockEntity nexus) {
        if (nexus.establishedAt == NOT_ESTABLISHED) {
            nexus.establishedAt = level.getGameTime();
            nexus.setChanged();
        }
        nexus.establish();
        nexus.networkState.tick(level, pos);
    }

    /**
     * On the server, the first time it runs, records this Nexus in the
     * directory of networks, where a copy of a Nexus that still stands gets a
     * network of its own, and takes up the access of the network it leads.
     */
    private void establish() {
        if (!guard.isEstablished() && level instanceof ServerLevel serverLevel) {
            final UUID led = guard.establish(serverLevel, worldPosition, network.id());
            if (!led.equals(network.id())) {
                network = new Network(led, network.name(), network.color());
                setChanged();
            }
        }
    }

    @Override
    public NetworkGuard guard() {
        return guard;
    }


    @Override
    public long establishedAt() {
        return establishedAt;
    }

    @Override
    public void showStatus(final NexusStatus status) {
        final BlockState state = getBlockState();
        if (level != null && state.getValue(NexusBlock.STATUS) != status) {
            level.setBlock(worldPosition, state.setValue(NexusBlock.STATUS, status), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * @return figures refreshed on the server; on the client always empty, the
     *         menu receives them by packet instead
     */
    public NetworkStatistics statistics() {
        return networkState.statistics();
    }

    @Override
    public EnergyBuffer energy() {
        return networkState.energy();
    }

    /**
     * @return the handler through which FE goes into the network and comes out of it; empty on the client
     */
    public EnergyHandler energyHandler() {
        return networkState.energyHandler();
    }

    @Override
    public <C extends NetworkComponent> C component(final NetworkComponentType<C> type) {
        return networkState.component(type);
    }

    @Override
    public void invalidateNetwork() {
        networkState.invalidate();
    }

    @Override
    public ClickGuard clickGuard() {
        return clickGuard;
    }

    public Container upgrades() {
        return upgrades.container();
    }

    /**
     * Broken, the Nexus drops its upgrade and lets go of its chunk.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            upgrades.dropAndRelease(level, pos);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        networkState.release();
    }

    @Override
    public void rename(final String newName) {
        network.rename(newName.isBlank() ? NexusNetworks.DEFAULT_NAME : newName);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void recolor(final NetworkColor newColor) {
        network.recolor(newColor);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            networkState.paintDevices(level, newColor);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nexus.nexus");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new NexusMenu(containerId, inventory, worldPosition);
    }

    @Override
    public void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_NETWORK_ID, UUIDUtil.CODEC, network.id());
        output.putString(TAG_NETWORK_NAME, network.name());
        output.putInt(TAG_NETWORK_COLOR, network.color().rgb());
        output.putLong(TAG_ESTABLISHED_AT, establishedAt);
        upgrades.save(output);
    }

    @Override
    public void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        final String name = input.getStringOr(TAG_NETWORK_NAME, network.name());
        final int color = input.getIntOr(TAG_NETWORK_COLOR, network.color().rgb());
        final UUID id = input.read(TAG_NETWORK_ID, UUIDUtil.CODEC).orElse(network.id());
        network = new Network(id, name, new NetworkColor(color));
        establishedAt = input.getLongOr(TAG_ESTABLISHED_AT, NOT_ESTABLISHED);
        upgrades.load(input);
    }

    /**
     * A Nexus placed from an item named on an anvil or after a rename starts its
     * network under that name; one placed again after it was broken goes on with
     * the id and color of its network too.
     */
    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        super.applyImplicitComponents(components);
        final Component custom = components.get(DataComponents.CUSTOM_NAME);
        String name = network.name();
        if (custom != null && !custom.getString().isBlank()) {
            final String named = custom.getString().strip();
            name = named.substring(0, Math.min(named.length(), Network.MAX_NAME_LENGTH));
        }
        final UUID carried = components.get(NexusDataComponents.NETWORK_ID.get());
        guard.carry(carried);
        network = new Network(carried != null ? carried : network.id(), name,
                components.getOrDefault(NexusDataComponents.NETWORK_COLOR.get(), network.color()));
    }

    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!network.name().equals(NexusNetworks.DEFAULT_NAME)) {
            components.set(DataComponents.CUSTOM_NAME, Component.literal(network.name()));
        }
        components.set(NexusDataComponents.NETWORK_ID.get(), network.id());
        components.set(NexusDataComponents.NETWORK_COLOR.get(), network.color());
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
