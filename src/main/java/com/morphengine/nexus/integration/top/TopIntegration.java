package com.morphengine.nexus.integration.top;

import com.morphengine.nexus.Nexus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Hands the plugin of Nexus to The One Probe, which asks mods for theirs through a message, when it is there. Nothing
 * of the mod is loaded when it is not.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class TopIntegration {

    private static final String PROBE_MOD = "theoneprobe";
    private static final String HANDSHAKE = "getTheOneProbe";

    private TopIntegration() {
    }

    @SubscribeEvent
    static void handOver(final InterModEnqueueEvent event) {
        if (ModList.get().isLoaded(PROBE_MOD)) {
            InterModComms.sendTo(PROBE_MOD, HANDSHAKE, NexusTopPlugin::new);
        }
    }
}
