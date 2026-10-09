package com.morphengine.nexus.networking;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.machine.MachineActivity;
import com.morphengine.nexus.menu.MachineView;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: a new view for the machine menu with the given container id.
 */
public record MachineViewPayload(int containerId, MachineView view) implements CustomPacketPayload {

    public static final Type<MachineViewPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "machine_view"));

    private static final StreamCodec<RegistryFriendlyByteBuf, MachineView> VIEW_CODEC =
            StreamCodec.of(MachineViewPayload::writeView, MachineViewPayload::readView);

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MachineViewPayload::containerId,
                    VIEW_CODEC, MachineViewPayload::view,
                    MachineViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void writeView(final RegistryFriendlyByteBuf buffer, final MachineView view) {
        buffer.writeVarLong(view.stored());
        buffer.writeVarLong(view.capacity());
        buffer.writeVarInt(view.speedPercent());
        buffer.writeEnum(view.activity());
        buffer.writeVarInt(view.progress().size());
        for (int percent : view.progress()) {
            buffer.writeVarInt(percent);
        }
        NetworkBadgeCodec.write(buffer, view.network());
        buffer.writeBoolean(view.tank() != null);
        if (view.tank() != null) {
            writeTank(buffer, view.tank());
        }
    }

    private static void writeTank(final RegistryFriendlyByteBuf buffer, final TankView tank) {
        buffer.writeBoolean(tank.fluid() != null);
        if (tank.fluid() != null) {
            FluidKey.STREAM_CODEC.encode(buffer, tank.fluid());
        }
        buffer.writeVarLong(tank.amount());
        buffer.writeVarLong(tank.capacity());
    }

    private static TankView readTank(final RegistryFriendlyByteBuf buffer) {
        final FluidKey fluid = buffer.readBoolean() ? FluidKey.STREAM_CODEC.decode(buffer) : null;
        return new TankView(fluid, buffer.readVarLong(), buffer.readVarLong());
    }

    private static MachineView readView(final RegistryFriendlyByteBuf buffer) {
        final long stored = buffer.readVarLong();
        final long capacity = buffer.readVarLong();
        final int speedPercent = buffer.readVarInt();
        final MachineActivity activity = buffer.readEnum(MachineActivity.class);
        final int lines = buffer.readVarInt();
        final List<Integer> progress = new ArrayList<>(lines);
        for (int line = 0; line < lines; line++) {
            progress.add(buffer.readVarInt());
        }
        final NetworkBadge network = NetworkBadgeCodec.read(buffer);
        return new MachineView(stored, capacity, speedPercent, activity, progress, network,
                buffer.readBoolean() ? readTank(buffer) : null);
    }
}
