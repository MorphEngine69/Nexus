package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.menu.BlueprintTerminalMenu;
import com.morphengine.nexus.menu.CraftingMonitorMenu;
import com.morphengine.nexus.menu.TerminalPanel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Payloads of autocrafting: the Blueprint Terminal's draft, crafting requests
 * from terminals and the Crafting Monitor. Each handler first checks that the
 * player still looks at the menu the payload names.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class AutocraftingPayloadHandlers {

    private AutocraftingPayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BlueprintSlotPayload.TYPE, BlueprintSlotPayload.STREAM_CODEC,
                (payload, context) -> onEncoder(context, payload.containerId(),
                        menu -> menu.setDraftSlot(payload.slot(), payload.resource())));
        registrar.playToServer(BlueprintAmountPayload.TYPE, BlueprintAmountPayload.STREAM_CODEC,
                (payload, context) -> onEncoder(context, payload.containerId(),
                        menu -> menu.setDraftAmount(payload.slot(), payload.amount())));
        registrar.playToServer(BlueprintRecipePayload.TYPE, BlueprintRecipePayload.STREAM_CODEC,
                (payload, context) -> onEncoder(context, payload.containerId(),
                        menu -> menu.replaceDraft(payload.draft())));
        registrar.playToServer(CraftRequestPayload.TYPE, CraftRequestPayload.STREAM_CODEC,
                (payload, context) -> onMenu(context, payload.containerId(), TerminalPanel.class,
                        panel -> {
                            if (context.player() instanceof ServerPlayer player) {
                                panel.terminal().requestCraft(player, payload.resource(), payload.amount(),
                                        payload.request());
                            }
                        }));
        registrar.playToServer(CancelTaskPayload.TYPE, CancelTaskPayload.STREAM_CODEC,
                (payload, context) -> onMenu(context, payload.containerId(), CraftingMonitorMenu.class,
                        menu -> menu.cancel(context.player(), payload.task())));
        registrar.playToClient(BlueprintDraftPayload.TYPE, BlueprintDraftPayload.STREAM_CODEC,
                (payload, context) -> onMenu(context, payload.containerId(), BlueprintTerminalMenu.class,
                        menu -> menu.acceptDraft(payload.draft(), payload.craftingOutputs())));
        registrar.playToClient(TerminalCraftablesPayload.TYPE, TerminalCraftablesPayload.STREAM_CODEC,
                (payload, context) -> onMenu(context, payload.containerId(), TerminalPanel.class,
                        panel -> panel.terminal().contents().acceptCraftables(payload.craftables())));
        registrar.playToClient(CraftPlanPayload.TYPE, CraftPlanPayload.STREAM_CODEC,
                (payload, context) -> onMenu(context, payload.containerId(), TerminalPanel.class,
                        panel -> panel.terminal().contents().acceptPlan(payload.plan(), payload.outcome())));
        registrar.playToClient(CraftingMonitorPayload.TYPE, CraftingMonitorPayload.STREAM_CODEC,
                (payload, context) -> onMenu(context, payload.containerId(), CraftingMonitorMenu.class,
                        menu -> menu.acceptTasks(payload.tasks())));
    }

    private static <M> void onMenu(
            final IPayloadContext context, final int containerId, final Class<M> type, final Consumer<M> action) {
        context.enqueueWork(() -> {
            final M menu = openMenu(context.player().containerMenu, containerId, type);
            if (menu != null) {
                action.accept(menu);
            }
        });
    }

    /**
     * Changes to the draft of a Blueprint encoder, for a player who may configure the network.
     */
    private static void onEncoder(
            final IPayloadContext context, final int containerId, final Consumer<BlueprintTerminalMenu> action) {
        onMenu(context, containerId, BlueprintTerminalMenu.class, menu -> {
            if (menu.permits(context.player(), Permission.CONFIGURE)) {
                action.accept(menu);
            }
        });
    }

    private static <M> @Nullable M openMenu(
            final AbstractContainerMenu menu, final int containerId, final Class<M> type) {
        return menu.containerId == containerId && type.isInstance(menu) ? type.cast(menu) : null;
    }
}
