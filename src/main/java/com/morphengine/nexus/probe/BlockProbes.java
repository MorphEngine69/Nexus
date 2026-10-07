package com.morphengine.nexus.probe;

import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.CraftingMonitorBlockEntity;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.block.entity.NetworkDeviceBlockEntity;
import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.function.Function;

/**
 * The reports of the blocks of Nexus: what a tooltip mod shows for the block entity it points at. A device of a
 * network first shows what all of them do, then what is its own. Server thread only.
 */
public final class BlockProbes {

    private static final List<Description<?>> DESCRIPTIONS = List.of(
            new Description<>(NexusBlockEntity.class, PowerProbes::nexus),
            new Description<>(EnergyCellBlockEntity.class, PowerProbes::energyCell),
            new Description<>(MachineBlockEntity.class, PowerProbes::machine),
            new Description<>(GeneratorBlockEntity.class, PowerProbes::generator),
            new Description<>(StorageVaultBlockEntity.class, StorageProbes::storageVault),
            new Description<>(ExternalVaultBlockEntity.class, StorageProbes::externalVault),
            new Description<>(TransferDeviceBlockEntity.class, LogisticsProbes::transferDevice),
            new Description<>(AssemblerBlockEntity.class, LogisticsProbes::assembler),
            new Description<>(CraftingMonitorBlockEntity.class, LogisticsProbes::craftingMonitor),
            new Description<>(TerminalBlockEntity.class, LogisticsProbes::terminal),
            new Description<>(NetworkTransmitterBlockEntity.class, WirelessProbes::transmitter),
            new Description<>(NexusLinkBlockEntity.class, WirelessProbes::link));

    private BlockProbes() {
    }

    /**
     * @return what to show for {@code block}; empty for a block entity that is not one of Nexus
     */
    public static ProbeReport of(final BlockEntity block) {
        ProbeReport report = block instanceof NetworkDeviceBlockEntity device
                ? DeviceProbes.of(device) : ProbeReport.EMPTY;
        for (Description<?> description : DESCRIPTIONS) {
            report = report.and(description.describe(block));
        }
        return report;
    }

    private record Description<T extends BlockEntity>(Class<T> type, Function<T, ProbeReport> describer) {

        ProbeReport describe(final BlockEntity block) {
            return type.isInstance(block) ? describer.apply(type.cast(block)) : ProbeReport.EMPTY;
        }
    }
}
