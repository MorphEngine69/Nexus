package com.morphengine.nexus.external;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Tells the External Vaults near a block a player used that someone is at the block they lend, so that they read it
 * often while the player takes things out or puts them in by hand, and what the network knows of it keeps up.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class VaultWatch {

    /** How far from the block used a vault may stand and still lend a part of it, as the halves of a double chest. */
    private static final int REACH = 2;

    private VaultWatch() {
    }

    @SubscribeEvent
    static void onRightClick(final PlayerInteractEvent.RightClickBlock event) {
        final Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        final BlockPos used = event.getPos();
        for (BlockPos near : BlockPos.betweenClosed(used.offset(-REACH, -REACH, -REACH),
                used.offset(REACH, REACH, REACH))) {
            if (level.getBlockEntity(near) instanceof ExternalVaultBlockEntity vault) {
                vault.watch(level.getGameTime());
            }
        }
    }
}
