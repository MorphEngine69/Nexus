package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.NetworkBadgeView;
import com.morphengine.nexus.menu.TerminalPanel;
import com.morphengine.nexus.menu.VaultCellMenu;
import com.morphengine.nexus.resource.ResourceTypes;
import com.morphengine.nexus.terminal.TerminalSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Payloads of the storage: Vault Cell panels, terminals, and the network badge
 * shown by storage devices. Each handler first checks that the player still
 * looks at the menu the payload names.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class StoragePayloadHandlers {

    private StoragePayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CellRenamePayload.TYPE, CellRenamePayload.STREAM_CODEC,
                (payload, context) -> onCellMenu(context, payload.containerId(),
                        menu -> menu.rename(payload.name())));
        registrar.playToServer(CellFilterPayload.TYPE, CellFilterPayload.STREAM_CODEC,
                (payload, context) -> onCellMenu(context, payload.containerId(),
                        menu -> menu.setFilterSlot(payload.slot(), payload.resource())));
        registrar.playToServer(CellFilterModePayload.TYPE, CellFilterModePayload.STREAM_CODEC,
                (payload, context) -> onCellMenu(context, payload.containerId(), VaultCellMenu::toggleFilterMode));
        registrar.playToServer(TerminalClickPayload.TYPE, TerminalClickPayload.STREAM_CODEC,
                StoragePayloadHandlers::handleTerminalClick);
        registrar.playToServer(TerminalSettingsPayload.TYPE, TerminalSettingsPayload.STREAM_CODEC,
                StoragePayloadHandlers::handleTerminalSettings);
        registrar.playToServer(CraftingGridFillPayload.TYPE, CraftingGridFillPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (openMenu(context.player(), payload.containerId()) instanceof CraftingTerminalMenu menu) {
                        menu.fillGrid(payload.slots(), payload.amount());
                    }
                }));
        registrar.playToClient(TerminalContentsPayload.TYPE, TerminalContentsPayload.STREAM_CODEC,
                StoragePayloadHandlers::handleTerminalContents);
        registrar.playToClient(NetworkBadgePayload.TYPE, NetworkBadgePayload.STREAM_CODEC,
                StoragePayloadHandlers::handleBadge);
    }

    private static void onCellMenu(
            final IPayloadContext context, final int containerId, final Consumer<VaultCellMenu> action) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof VaultCellMenu menu && menu.containerId == containerId) {
                action.accept(menu);
            }
        });
    }

    private static void handleTerminalClick(final TerminalClickPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final AbstractContainerMenu menu = openMenu(context.player(), payload.containerId());
            if (menu instanceof TerminalPanel panel && context.player() instanceof ServerPlayer player) {
                panel.terminal().click(player, menu, payload.resource(), payload.click());
            }
        });
    }

    /**
     * Saves the new settings on the terminal; the client has already applied them.
     */
    private static void handleTerminalSettings(final TerminalSettingsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final TerminalSettings settings = payload.settings();
            final AbstractContainerMenu menu = openMenu(context.player(), payload.containerId());
            if (!(menu instanceof TerminalPanel panel)
                    || !(panel.terminal().binding().blockEntity() instanceof TerminalBlockEntity terminal)
                    || settings.shownType() != null && !ResourceTypes.REGISTRY.containsKey(settings.shownType())) {
                return;
            }
            terminal.changeSettings(settings);
        });
    }

    private static void handleTerminalContents(final TerminalContentsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (openMenu(context.player(), payload.containerId()) instanceof TerminalPanel panel) {
                panel.terminal().contents().accept(payload.status(), payload.reset(), payload.entries());
            }
        });
    }

    private static void handleBadge(final NetworkBadgePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (openMenu(context.player(), payload.containerId()) instanceof NetworkBadgeView view) {
                view.acceptBadge(payload.badge());
            }
        });
    }

    /**
     * @return the menu the player has open, if it is the one with {@code containerId}
     */
    private static @Nullable AbstractContainerMenu openMenu(final Player player, final int containerId) {
        final AbstractContainerMenu menu = player.containerMenu;
        return menu.containerId == containerId ? menu : null;
    }
}
