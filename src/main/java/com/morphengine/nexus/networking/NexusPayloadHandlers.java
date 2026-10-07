package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.Renamable;
import com.morphengine.nexus.menu.AnalyserMenu;
import com.morphengine.nexus.menu.DevicePanel;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.GeneratorMenu;
import com.morphengine.nexus.menu.MachineMenu;
import com.morphengine.nexus.menu.NexusMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class NexusPayloadHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusPayloadHandlers.class);

    private NexusPayloadHandlers() {
    }

    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                DeviceRenamePayload.TYPE, DeviceRenamePayload.STREAM_CODEC, NexusPayloadHandlers::handleRename);
        registrar.playToServer(
                NexusRecolorPayload.TYPE, NexusRecolorPayload.STREAM_CODEC, NexusPayloadHandlers::handleRecolor);
        registrar.playToServer(NetworkAccessEditPayload.TYPE, NetworkAccessEditPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleAccessEdit);
        registrar.playToClient(NetworkAccessPayload.TYPE, NetworkAccessPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleAccess);
        registrar.playToClient(NexusStatisticsPayload.TYPE, NexusStatisticsPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleStatistics);
        registrar.playToClient(
                AnalyserViewPayload.TYPE, AnalyserViewPayload.STREAM_CODEC, NexusPayloadHandlers::handleAnalyserView);
        registrar.playToClient(
                NexusEnergyPayload.TYPE, NexusEnergyPayload.STREAM_CODEC, NexusPayloadHandlers::handleEnergy);
        registrar.playToServer(NexusEnergyTabPayload.TYPE, NexusEnergyTabPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleEnergyTab);
        registrar.playToClient(
                EnergyCellViewPayload.TYPE, EnergyCellViewPayload.STREAM_CODEC, NexusPayloadHandlers::handleCellView);
        registrar.playToClient(GeneratorViewPayload.TYPE, GeneratorViewPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleGeneratorView);
        registrar.playToClient(MachineViewPayload.TYPE, MachineViewPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleMachineView);
    }

    private static void handleRename(final DeviceRenamePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof DevicePanel panel)
                    || !panel.binding().pos().equals(payload.pos())
                    || !(panel.binding().blockEntity() instanceof Renamable device)
                    || !panel.binding().permits(context.player(), Permission.CONFIGURE)) {
                return;
            }
            try {
                device.rename(payload.name());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Rejected device rename at {} from {}: {}",
                        payload.pos(), context.player().getName().getString(), e.getMessage());
            }
        });
    }

    private static void handleRecolor(final NexusRecolorPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final NexusBlockEntity nexus = editingNexus(context.player(), payload.pos());
            if (nexus == null) {
                return;
            }
            try {
                nexus.recolor(new NetworkColor(payload.rgb()));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Rejected network color at {} from {}: {}",
                        payload.pos(), context.player().getName().getString(), e.getMessage());
            }
        });
    }

    private static void handleAccessEdit(final NetworkAccessEditPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof NexusMenu menu
                    && menu.pos().equals(payload.pos())) {
                menu.edit(player, payload.edit());
            }
        });
    }

    private static void handleAccess(final NetworkAccessPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof NexusMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptAccess(payload.view());
            }
        });
    }

    private static void handleStatistics(final NexusStatisticsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof NexusMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptStatistics(payload.statistics());
            }
        });
    }

    private static void handleAnalyserView(final AnalyserViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof AnalyserMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.acceptView(payload.view());
            }
        });
    }

    private static void handleEnergy(final NexusEnergyPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof NexusMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptEnergy(payload.report());
            }
        });
    }

    private static void handleEnergyTab(final NexusEnergyTabPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof NexusMenu menu && menu.containerId == payload.containerId()) {
                menu.showEnergy(payload.shown());
            }
        });
    }

    private static void handleCellView(final EnergyCellViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof EnergyCellMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.acceptView(payload.view());
            }
        });
    }

    private static void handleMachineView(final MachineViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof MachineMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.acceptView(payload.view());
            }
        });
    }

    private static void handleGeneratorView(final GeneratorViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof GeneratorMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.acceptView(payload.view());
            }
        });
    }

    /**
     * @return the Nexus at {@code pos} while {@code player} looks at its panel
     *         and may configure it; {@code null} otherwise
     */
    private static @Nullable NexusBlockEntity editingNexus(final Player player, final BlockPos pos) {
        if (player.containerMenu instanceof NexusMenu menu && menu.pos().equals(pos)
                && menu.permits(player, Permission.CONFIGURE)) {
            return menu.blockEntity();
        }
        return null;
    }
}
