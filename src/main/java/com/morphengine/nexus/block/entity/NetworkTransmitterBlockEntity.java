package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.block.WirelessBlock;
import com.morphengine.nexus.item.NetworkCardItem;
import com.morphengine.nexus.level.ChunkAnchors;
import com.morphengine.nexus.level.NetworkChanges;
import com.morphengine.nexus.level.UpgradeHolder;
import com.morphengine.nexus.level.WirelessLinks;
import com.morphengine.nexus.menu.NetworkTransmitterMenu;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * A Network Transmitter: carries its network to the Network Receiver the
 * Network Card in its slot is linked to; a Chunk Loader Upgrade in its other
 * slot keeps its chunk loaded, as the link needs. It keeps the world's {@link
 * WirelessLinks} in step with its card and tells both sides of a link that
 * appears or goes away. Once a second it works out what its panel shows and
 * lights its block while the link goes through and the network has energy.
 */
public final class NetworkTransmitterBlockEntity extends AnimatedDeviceBlockEntity implements UpgradeHolder, Renamable {

    public static final int UPGRADE_SLOTS = 1;
    public static final UpgradeLimits UPGRADE_LIMITS = new UpgradeLimits(Map.of(UpgradeTypes.CHUNK_LOADER, 1));

    private static final int STATE_CHECK_INTERVAL_TICKS = 20;
    private static final String TAG_CARD = "card";
    private static final String TAG_UPGRADES = "upgrades";

    private final SimpleContainer card = new CardSlot();
    private final UpgradeContainer upgrades = new UpgradeContainer(UPGRADE_SLOTS, UPGRADE_LIMITS,
            this::upgradesChanged);
    private TransmitterStatus status = TransmitterStatus.NO_CARD;

    public NetworkTransmitterBlockEntity(final BlockPos pos, final BlockState state) {
        super(NexusBlockEntityTypes.NETWORK_TRANSMITTER.get(), pos, state, WirelessBlock::animationOf);
    }

    public static void serverTick(
            final Level level, final BlockPos pos, final BlockState state,
            final NetworkTransmitterBlockEntity transmitter) {
        if (level.getGameTime() % STATE_CHECK_INTERVAL_TICKS != 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        transmitter.status = transmitter.statusIn(serverLevel.getServer());
        final boolean active = transmitter.status == TransmitterStatus.LINKED && transmitter.isNetworkPowered();
        if (state.getValue(WirelessBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(WirelessBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    public Container card() {
        return card;
    }

    public Container upgrades() {
        return upgrades;
    }

    private void upgradesChanged() {
        ChunkAnchors.follow(this, upgrades);
        setChanged();
    }

    public TransmitterStatus status() {
        return status;
    }

    private TransmitterStatus statusIn(final MinecraftServer server) {
        final GlobalPos receiver = NetworkCardItem.receiverOf(card.getItem(0));
        if (receiver == null) {
            return TransmitterStatus.NO_CARD;
        }
        final ServerLevel at = server.getLevel(receiver.dimension());
        return at != null && at.isLoaded(receiver.pos()) && WirelessBlock.isLinkEnd(at.getBlockState(receiver.pos()))
                ? TransmitterStatus.LINKED : TransmitterStatus.UNREACHABLE;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        relink();
    }

    /**
     * Brings the world's links in step with the card, and when the link changed
     * tells the networks at this end and at the receiver it had before.
     */
    private void relink() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        final WirelessLinks links = WirelessLinks.of(serverLevel.getServer());
        final GlobalPos self = GlobalPos.of(serverLevel.dimension(), worldPosition);
        final GlobalPos before = links.receiverOf(self);
        final GlobalPos now = NetworkCardItem.receiverOf(card.getItem(0));
        if (Objects.equals(before, now)) {
            return;
        }
        if (now != null) {
            links.link(self, now);
        } else {
            links.unlink(self);
        }
        NetworkChanges.linkChanged(serverLevel, worldPosition);
        tellReceiver(serverLevel.getServer(), before);
    }

    private static void tellReceiver(final MinecraftServer server, final @Nullable GlobalPos receiver) {
        final ServerLevel at = receiver != null ? server.getLevel(receiver.dimension()) : null;
        if (receiver != null && at != null && at.isLoaded(receiver.pos())) {
            NetworkChanges.linkChanged(at, receiver.pos());
        }
    }

    @Override
    public void rename(final String newName) {
        changeName(newName);
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new NetworkTransmitterMenu(containerId, inventory, worldPosition);
    }

    /**
     * Broken, the transmitter drops its card and upgrade, lets go of its chunk
     * and ends its link.
     */
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ChunkAnchors.release(this);
        if (level != null) {
            Containers.dropContents(level, pos, upgrades);
            Containers.dropContents(level, pos, card);
            card.clearContent();
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueOutput output = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        output.saveItems(TAG_CARD, card.getItems());
        output.saveItems(TAG_UPGRADES, upgrades.getItems());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueInput input = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        input.loadItems(TAG_CARD, card.getItems());
        input.loadItems(TAG_UPGRADES, upgrades.getItems());
    }

    /** The card slot: takes one Network Card; any change relinks the transmitter. */
    private final class CardSlot extends SimpleContainer {

        CardSlot() {
            super(1);
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return stack.getItem() instanceof NetworkCardItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            NetworkTransmitterBlockEntity.this.setChanged();
            relink();
        }
    }
}
