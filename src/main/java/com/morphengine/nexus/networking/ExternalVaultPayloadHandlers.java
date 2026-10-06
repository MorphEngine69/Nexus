package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.ExternalVaultMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payloads of the External Vault panel. The handler first checks that the player still looks at the menu the payload
 * names.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ExternalVaultPayloadHandlers {

    private ExternalVaultPayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ExternalVaultSettingsPayload.TYPE, ExternalVaultSettingsPayload.STREAM_CODEC,
                (payload, context) -> onVaultMenu(context, payload));
    }

    private static void onVaultMenu(final IPayloadContext context, final ExternalVaultSettingsPayload payload) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            if (player.containerMenu instanceof ExternalVaultMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptSettings(payload.settings());
            }
        });
    }
}
