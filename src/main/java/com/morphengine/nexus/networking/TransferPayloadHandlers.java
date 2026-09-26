package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.TransferDeviceMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

/**
 * Payloads of the Puller and Pusher panels. Each handler first checks that the
 * player still looks at the menu the payload names.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class TransferPayloadHandlers {

    private TransferPayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(KeepAmountPayload.TYPE, KeepAmountPayload.STREAM_CODEC,
                (payload, context) -> onTransferMenu(context, payload.containerId(),
                        menu -> menu.setKeepAmount(payload.slot(), payload.amount())));
        registrar.playToClient(TransferSettingsPayload.TYPE, TransferSettingsPayload.STREAM_CODEC,
                (payload, context) -> onTransferMenu(context, payload.containerId(),
                        menu -> menu.acceptSettings(payload.settings())));
    }

    private static void onTransferMenu(
            final IPayloadContext context, final int containerId, final Consumer<TransferDeviceMenu> action) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            if (player.containerMenu instanceof TransferDeviceMenu menu && menu.containerId == containerId) {
                action.accept(menu);
            }
        });
    }
}
