package com.morphengine.nexus.integration.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * What The One Probe shows for the blocks of Nexus, from the report in {@link com.morphengine.nexus.probe.BlockProbes}.
 */
final class NexusTopPlugin implements Function<ITheOneProbe, Void> {

    @Override
    public @Nullable Void apply(final ITheOneProbe probe) {
        probe.registerProvider(new ProbeInfoProvider());
        probe.registerProbeConfigProvider(new OwnBarsOnly());
        return null;
    }
}
