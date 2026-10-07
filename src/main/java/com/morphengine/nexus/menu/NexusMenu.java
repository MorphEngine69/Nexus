package com.morphengine.nexus.menu;

import com.morphengine.nexus.access.AccessRequests;
import com.morphengine.nexus.access.Operators;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.entity.DeviceUpgrades;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.networking.NetworkAccessPayload;
import com.morphengine.nexus.networking.NexusStatisticsPayload;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.security.EditResult;
import com.morphengine.nexus.security.NetworkSecurity;
import com.morphengine.nexus.security.SecurityEdit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Nexus panel: the network's figures, the upgrade slots and the player's
 * inventory, and who may do what with the network. The access is sent to the
 * client when it changes and once a second for the players online; changes
 * to it come back as edits, which the network's rules decide.
 */
public final class NexusMenu extends DeviceMenu<NexusBlockEntity> {

    public static final int UPGRADES_LEFT = 208;
    public static final int UPGRADES_TOP = 39;
    public static final int INVENTORY_LEFT = 37;
    public static final int INVENTORY_TOP = 198;

    private static final int ACCESS_INTERVAL_TICKS = 20;

    /** On the server the figures last sent, on the client the figures last received. */
    private NetworkStatistics statistics = NetworkStatistics.EMPTY;
    /** On the server the access last sent, on the client the access last received; {@code null} before. */
    private @Nullable AccessView access;
    private int accessRevision = -1;
    private int ticks;

    public NexusMenu(final int containerId, final Inventory inventory, final BlockPos pos) {
        super(NexusMenuTypes.NEXUS.get(), containerId, inventory, pos, NexusBlockEntity.class);
        final NexusBlockEntity nexus = blockEntity();
        UpgradeColumn.slots(viewer() != null && nexus != null ? nexus.upgrades() : null, DeviceUpgrades.LIMITS,
                UPGRADES_LEFT, UPGRADES_TOP)
                .forEach(this::addSlot);
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
    }

    public NetworkStatistics statistics() {
        return statistics;
    }

    public void acceptStatistics(final NetworkStatistics received) {
        statistics = received;
    }

    /**
     * @return who may do what with the network, as last received; {@code null}
     *         until the server has sent it
     */
    public @Nullable AccessView access() {
        return access;
    }

    public void acceptAccess(final AccessView received) {
        access = received;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        final ServerPlayer viewer = viewer();
        final NexusBlockEntity nexus = blockEntity();
        if (viewer == null || nexus == null) {
            return;
        }
        final NetworkStatistics current = nexus.statistics();
        if (!current.equals(statistics)) {
            statistics = current;
            PacketDistributor.sendToPlayer(viewer, new NexusStatisticsPayload(containerId, current));
        }
        sendAccess(viewer, nexus);
    }

    /**
     * Sends the access when it changed, or once a second, when the players
     * online may have changed, if the view did.
     */
    private void sendAccess(final ServerPlayer viewer, final NexusBlockEntity nexus) {
        final NetworkSecurity security = nexus.security();
        if (security.revision() == accessRevision && ticks++ % ACCESS_INTERVAL_TICKS != 0) {
            return;
        }
        accessRevision = security.revision();
        final AccessView current = AccessView.of(security, viewer, nexus.getBlockPos());
        if (!current.equals(access)) {
            access = current;
            PacketDistributor.sendToPlayer(viewer, new NetworkAccessPayload(containerId, current));
        }
    }

    /**
     * Makes a change to the network's access the player asked for, if they may.
     * A player to add must be on the server; names are taken from the server,
     * not from the request ({@link AccessRequests}). Server side only.
     */
    public void edit(final ServerPlayer player, final SecurityEdit requested) {
        final NexusBlockEntity nexus = blockEntity();
        final SecurityEdit edit = AccessRequests.trusted(player, requested);
        if (nexus == null) {
            return;
        }
        if (edit == null) {
            player.sendOverlayMessage(Component.translatable("gui.nexus.access.result.offline")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        final EditResult result = nexus.security().apply(Operators.editorOf(player), edit);
        if (result == EditResult.APPLIED && edit instanceof SecurityEdit.Claim) {
            nexus.invalidateNetwork();
        }
        if (result != EditResult.APPLIED && result != EditResult.UNCHANGED) {
            player.sendOverlayMessage(Component.translatable(
                    "gui.nexus.access.result." + result.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        final boolean isUpgrade = UpgradeColumn.takes(DeviceUpgrades.LIMITS, slots.get(slotIndex).getItem());
        return shiftClick(slotIndex, DeviceUpgrades.SIZE, 0, isUpgrade ? DeviceUpgrades.SIZE : 0);
    }
}
