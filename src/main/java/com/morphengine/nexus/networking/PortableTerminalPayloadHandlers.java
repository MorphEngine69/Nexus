package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.menu.TerminalSlot;
import com.morphengine.nexus.menu.TerminalSlots;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = Nexus.MOD_ID)
final class PortableTerminalPayloadHandlers {

    private PortableTerminalPayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(PortableTerminalPayload.TYPE, PortableTerminalPayload.STREAM_CODEC,
                PortableTerminalPayloadHandlers::handle);
    }

    private static void handle(final PortableTerminalPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final TerminalSlot slot = TerminalSlots.find(player);
            if (slot == null) {
                player.sendOverlayMessage(Component.translatable("item.nexus.nexus_terminal.none"));
                return;
            }
            switch (payload.action()) {
                case OPEN -> NexusTerminalItem.open(player, slot);
                case SWITCH_MODE -> NexusTerminalItem.cycleMode(player, slot.stackOf(player));
            }
        });
    }
}
