package com.morphengine.nexus.access;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.NetworkBlock;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.world.FrontSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Keeps the blocks of a network in the hands of those it lets build: breaking
 * a network block, or placing one where it connects to a network, takes
 * {@link Permission#BUILD} in every network concerned, and a Nexus, which
 * carries its whole network, takes {@link Permission#MANAGE} in its own. A
 * player may always take down a device they placed themselves. Players
 * placing or breaking through a device of the mod, or a fake player of
 * another mod, count as whoever the device works for; a placement nobody
 * stands behind counts as a stranger. Operators are never stopped.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class NetworkProtection {

    private NetworkProtection() {
    }

    @SubscribeEvent
    static void onBreak(final BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof NetworkBlock)
                || !(event.getPlayer() instanceof ServerPlayer player) || isUnrestricted(player)) {
            return;
        }
        final Permission refused = refusalToBreak(level, event.getPos(), player.getUUID());
        if (refused != null) {
            event.setCanceled(true);
            NetworkAccess.refuse(player, refused);
        }
    }

    @SubscribeEvent
    static void onPlace(final BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlacedBlock().getBlock() instanceof NetworkBlock)) {
            return;
        }
        final ServerPlayer player = event.getEntity() instanceof ServerPlayer placer ? placer : null;
        if (player != null && isUnrestricted(player)) {
            return;
        }
        final Permission refused = refusalToPlace(level, event.getPos(), player != null ? player.getUUID()
                : Util.NIL_UUID);
        if (refused != null) {
            event.setCanceled(true);
            if (player != null) {
                NetworkAccess.refuse(player, refused);
            }
        }
    }

    /**
     * Members are listed by the name they had when last seen; a player who
     * logs in under a new name is shown under it from now on.
     */
    @SubscribeEvent
    static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkSecurityData.of(player.level().getServer()).refreshNames(player.nameAndId());
        }
    }

    private static boolean isUnrestricted(final ServerPlayer player) {
        return FrontSpace.standsInForNobody(player) || Operators.isOperator(player);
    }

    /**
     * @return the permission {@code player} lacks to break the network block at
     *         {@code pos}; {@code null} when they may
     */
    private static @Nullable Permission refusalToBreak(final ServerLevel level, final BlockPos pos, final UUID player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof NexusBlockEntity nexus && !nexus.security().isAllowed(player, Permission.MANAGE)) {
            return Permission.MANAGE;
        }
        if (!(blockEntity instanceof NexusBlockEntity) && blockEntity instanceof Secured device
                && device.isOwnedBy(player)) {
            return null;
        }
        return mayBuildAround(level, pos, player, null) ? null : Permission.BUILD;
    }

    /**
     * @return the permission {@code player} lacks for the network block just
     *         placed at {@code pos}; {@code null} when they may place it
     */
    private static @Nullable Permission refusalToPlace(final ServerLevel level, final BlockPos pos, final UUID player) {
        if (level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            final AccessPolicy inherited = nexus.guard().inheritedPolicy(level, pos);
            if (inherited != null && !inherited.isAllowed(player, Permission.MANAGE)) {
                return Permission.MANAGE;
            }
        }
        return mayBuildAround(level, pos, player, GlobalPos.of(level.dimension(), pos)) ? null : Permission.BUILD;
    }

    private static boolean mayBuildAround(
            final ServerLevel level, final BlockPos pos, final UUID player, final @Nullable GlobalPos excluded) {
        for (AccessPolicy rules : NetworkTerritory.rulesAround(level, pos, excluded)) {
            if (!rules.isAllowed(player, Permission.BUILD)) {
                return false;
            }
        }
        return true;
    }
}
