package com.morphengine.nexus.integration.top;

import com.morphengine.nexus.block.NetworkDeviceBlock;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns off the energy and tank bars The One Probe draws on its own for the blocks of Nexus, since the report of the
 * block has them, and two of each would be shown otherwise.
 */
final class OwnBarsOnly implements IProbeConfigProvider {

    private static final int OFF = 0;

    @Override
    public void getProbeConfig(
            final IProbeConfig config, final Player player, final Level level, final Entity entity,
            final IProbeHitEntityData data) {
        // Entities of Nexus have nothing to show.
    }

    @Override
    public void getProbeConfig(
            final IProbeConfig config, final Player player, final Level level, final BlockState state,
            final IProbeHitData data) {
        if (state.getBlock() instanceof NetworkDeviceBlock) {
            config.setRFMode(OFF);
            config.setTankMode(OFF);
        }
    }
}
