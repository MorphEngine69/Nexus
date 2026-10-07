package com.morphengine.nexus.analysis;

import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.resource.AmountUnit;
import com.morphengine.nexus.transfer.TransferSettings;
import com.morphengine.nexus.transport.TransferRate;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * What a Puller, Pusher, Placer or Remover does: whether it works, what it moves, how often and how much each time,
 * which comes to how much a second, upgrades taken into account.
 */
public final class TransferAnalysis {

    private static final int TICKS_PER_SECOND = 20;

    private TransferAnalysis() {
    }

    public static void describe(final TransferDeviceBlockEntity device, final AnalyserLines lines) {
        final TransferSettings settings = device.settings();
        final TransferRate rate = device.rate();
        final AmountUnit unit = settings.resource().resourceType().unit();
        final long perOperation = unit.step() * rate.multiplier();
        final double operationsPerSecond = (double) TICKS_PER_SECOND / rate.intervalTicks();

        lines.heading("transfer");
        lines.addTranslated("status", "status." + statusOf(device));
        lines.addTranslated("moves", "resource." + settings.resource().getSerializedName());
        lines.addTranslated("interval", "ticks", rate.intervalTicks());
        lines.addTranslated("operations", "per_second", String.format(Locale.ROOT, "%.2f", operationsPerSecond));
        lines.add("per_operation", unit.quantity(perOperation));
        lines.add("throughput", Component.empty().append(unit.quantity(Math.round(perOperation * operationsPerSecond)))
                .append(Component.translatable("gui.nexus.analyser.per_second_suffix")));
        CommonLines.addUpgrades(lines, device.upgrades());
    }

    private static String statusOf(final TransferDeviceBlockEntity device) {
        if (!device.isNetworkPowered()) {
            return "no_power";
        }
        if (device.missingPermission() != null) {
            return "halted";
        }
        return device.isPausedByRedstone() ? "paused" : "working";
    }
}
