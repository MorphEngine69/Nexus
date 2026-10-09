package com.morphengine.nexus.integration.jade;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.probe.ProbeLine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

/**
 * The text lines of a block's report, made on the server and sent in the data Jade asks for. Jade does not let a
 * provider of data also be a provider of the tooltip, so the client shows them through {@link Client}, which reads
 * what this one sent.
 */
class ProbeTextProvider implements StreamServerDataProvider<BlockAccessor, List<Component>> {

    static final ProbeTextProvider SERVER = new ProbeTextProvider();

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "probe");
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> CODEC =
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list());

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public List<Component> streamData(final BlockAccessor accessor) {
        return ProbeBars.linesOf(accessor, ProbeLine.Text.class).stream().map(ProbeLine.Text::text).toList();
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, List<Component>> streamCodec() {
        return CODEC;
    }

    @Override
    public boolean shouldRequestData(final BlockAccessor accessor) {
        return !ProbeBars.linesOf(accessor, ProbeLine.Text.class).isEmpty();
    }

    /**
     * Adds the lines the server sent to the tooltip.
     */
    static final class Client extends ProbeTextProvider implements IBlockComponentProvider {

        static final Client TOOLTIP = new Client();

        @Override
        public void appendTooltip(final ITooltip tooltip, final BlockAccessor accessor, final IPluginConfig config) {
            decodeFromData(accessor).ifPresent(lines -> lines.forEach(tooltip::add));
        }
    }
}
