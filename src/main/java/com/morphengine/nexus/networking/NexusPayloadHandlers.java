package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.Renamable;
import com.morphengine.nexus.menu.CoalGeneratorMenu;
import com.morphengine.nexus.menu.DeviceMenu;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.menu.NexusMenu;
import net.minecraft.core.BlockPos;
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
        registrar.playToClient(NexusStatisticsPayload.TYPE, NexusStatisticsPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleStatistics);
        registrar.playToClient(
                EnergyCellViewPayload.TYPE, EnergyCellViewPayload.STREAM_CODEC, NexusPayloadHandlers::handleCellView);
        registrar.playToClient(CoalGeneratorViewPayload.TYPE, CoalGeneratorViewPayload.STREAM_CODEC,
                NexusPayloadHandlers::handleGeneratorView);
    }

    private static void handleRename(final DeviceRenamePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof DeviceMenu<?> menu) || !menu.pos().equals(payload.pos())
                    || !(menu.blockEntity() instanceof Renamable device)) {
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

    private static void handleStatistics(final NexusStatisticsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof NexusMenu menu && menu.containerId == payload.containerId()) {
                menu.acceptStatistics(payload.statistics());
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

    private static void handleGeneratorView(final CoalGeneratorViewPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof CoalGeneratorMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.acceptView(payload.view());
            }
        });
    }

    private static @Nullable NexusBlockEntity editingNexus(final Player player, final BlockPos pos) {
        if (player.containerMenu instanceof NexusMenu menu && menu.pos().equals(pos)) {
            return menu.blockEntity();
        }
        return null;
    }
}
