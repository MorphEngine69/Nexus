package com.morphengine.nexus.integration.jade;

import com.morphengine.nexus.block.NetworkDeviceBlock;
import com.morphengine.nexus.block.entity.NetworkDeviceBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * What Jade shows for the blocks of Nexus: text lines and bars of energy, fluid and progress, all from the report in
 * {@link com.morphengine.nexus.probe.BlockProbes}. Loaded by Jade alone; nothing else refers to these classes.
 */
@WailaPlugin
public final class NexusJadePlugin implements IWailaPlugin {

    @Override
    public void register(final IWailaCommonRegistration registration) {
        for (Class<?> block : new Class<?>[] {NetworkDeviceBlockEntity.class, NexusBlockEntity.class}) {
            registration.registerBlockDataProvider(ProbeTextProvider.SERVER, block);
            registration.registerEnergyStorage(ProbeBars.ENERGY, block);
            registration.registerFluidStorage(ProbeBars.TANKS, block);
            registration.registerProgress(ProbeBars.PROGRESS, block);
        }
    }

    @Override
    public void registerClient(final IWailaClientRegistration registration) {
        registration.registerBlockComponent(ProbeTextProvider.Client.TOOLTIP, NetworkDeviceBlock.class);
        registration.registerEnergyStorageClient(ProbeBars.ENERGY);
        registration.registerFluidStorageClient(ProbeBars.TANKS);
        registration.registerProgressClient(ProbeBars.PROGRESS);
    }
}
