package com.morphengine.nexus.probe;

import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.menu.GeneratorView;
import com.morphengine.nexus.menu.MachineView;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.menu.TankView;
import com.morphengine.nexus.resource.FluidKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;

/**
 * What the Nexus, the Energy Cell, the machines and the generators show: energy first, then what they are doing.
 */
final class PowerProbes {

    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance();
    private static final int FULL = 100;

    private PowerProbes() {
    }

    static ProbeReport nexus(final NexusBlockEntity nexus) {
        final ProbeReport.Builder report = ProbeReport.builder();
        final NetworkBadge badge = new NetworkBadge(nexus.network().name(), nexus.network().color());
        report.text(DeviceProbes.network(badge));
        if (nexus.getBlockState().getValue(NexusBlock.STATUS) == NexusStatus.CONFLICT) {
            report.text(Component.translatable("tooltip.nexus.probe.conflict").withStyle(ChatFormatting.RED));
        }
        final NetworkStatistics statistics = nexus.statistics();
        report.energy(statistics.energyStored(), statistics.energyCapacity());
        report.text(Component.translatable("gui.nexus.stats.input", NUMBERS.format(statistics.energyInput())));
        report.text(Component.translatable("gui.nexus.stats.output", NUMBERS.format(statistics.energyOutput())));
        report.text(Component.translatable("gui.nexus.stats.devices", statistics.devices()));
        return report.build();
    }

    static ProbeReport energyCell(final EnergyCellBlockEntity cell) {
        return ProbeReport.builder()
                .energy(cell.energyBuffer().stored(), cell.energyBuffer().capacity())
                .text(Component.translatable("gui.nexus.vault.priority", cell.energyPriority()))
                .build();
    }

    static ProbeReport machine(final MachineBlockEntity machine) {
        final MachineView view = machine.view();
        final ProbeReport.Builder report = ProbeReport.builder();
        report.text(DeviceProbes.named("gui.nexus.machine.status.", view.activity()));
        final int progress = view.progress().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (progress > 0) {
            report.progress(Component.translatable("tooltip.nexus.probe.progress"), progress);
        }
        report.energy(view.stored(), view.capacity());
        report.text(Component.translatable("gui.nexus.machine.speed", view.speedPercent()));
        if (view.tank() != null) {
            report.tank(stackOf(view.tank()), view.tank().capacity(), nameOf(view.tank(), null));
        }
        return report.build();
    }

    static ProbeReport generator(final GeneratorBlockEntity generator) {
        final GeneratorView view = generator.view();
        final ProbeReport.Builder report = ProbeReport.builder();
        report.text(switch (view.status()) {
            case GENERATING -> Component.translatable("gui.nexus.generator.generating",
                    NUMBERS.format(view.production()));
            case BUFFER_FULL -> Component.translatable("gui.nexus.generator.full");
            case NO_FUEL -> Component.translatable("gui.nexus.generator.no_fuel");
        });
        if (view.burnTicksTotal() > 0 && view.tanks().isEmpty()) {
            report.progress(Component.translatable("tooltip.nexus.probe.fuel"),
                    view.burnTicksLeft() * FULL / view.burnTicksTotal());
        }
        report.energy(view.stored(), view.capacity());
        for (int index = 0; index < view.tanks().size(); index++) {
            final TankView tank = view.tanks().get(index);
            final FluidKey accepted = index < view.accepted().size() ? view.accepted().get(index) : null;
            report.tank(stackOf(tank), tank.capacity(), nameOf(tank, accepted));
        }
        return report.build();
    }

    /**
     * @return the fluid in the tank, or the one it takes when it is empty, or just a tank when that is not known
     */
    private static Component nameOf(final TankView tank, final @Nullable FluidKey accepted) {
        if (tank.fluid() != null) {
            return tank.fluid().name();
        }
        return accepted != null ? accepted.name() : Component.translatable("tooltip.nexus.probe.tank");
    }

    private static FluidStack stackOf(final TankView tank) {
        return tank.fluid() == null ? FluidStack.EMPTY : tank.fluid().toStack((int) tank.amount());
    }
}
