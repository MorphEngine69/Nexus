package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.menu.FilterMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

/**
 * Payloads of every panel with a filter. Each handler first checks that the
 * player still looks at the menu the payload names.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class FilterPayloadHandlers {

    private FilterPayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(FilterSlotPayload.TYPE, FilterSlotPayload.STREAM_CODEC,
                (payload, context) -> onFilterMenu(context, payload.containerId(),
                        menu -> menu.setFilterSlot(payload.slot(), payload.resource())));
        registrar.playToServer(FilterModePayload.TYPE, FilterModePayload.STREAM_CODEC,
                (payload, context) -> onFilterMenu(context, payload.containerId(), FilterMenu::toggleFilterMode));
    }

    private static void onFilterMenu(
            final IPayloadContext context, final int containerId, final Consumer<FilterMenu> action) {
        context.enqueueWork(() -> {
            final AbstractContainerMenu menu = context.player().containerMenu;
            if (menu instanceof FilterMenu filterMenu && menu.containerId == containerId) {
                action.accept(filterMenu);
            }
        });
    }
}
