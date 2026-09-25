package com.morphengine.nexus;

import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusCreativeTabs;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.registry.NexusMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Nexus.MOD_ID)
public final class Nexus {

    public static final String MOD_ID = "nexus";

    public Nexus(final IEventBus modBus) {
        registerContent(modBus);
    }

    private void registerContent(final IEventBus modBus) {
        NexusBlocks.BLOCKS.register(modBus);
        NexusItems.ITEMS.register(modBus);
        NexusBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modBus);
        NexusMenuTypes.MENU_TYPES.register(modBus);
        NexusCreativeTabs.CREATIVE_TABS.register(modBus);
    }
}
