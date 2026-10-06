package com.morphengine.nexus;

import com.morphengine.nexus.registry.NexusBlockEntityTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusCreativeTabs;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusFluids;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.registry.NexusMaterials;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.registry.NexusMetals;
import com.morphengine.nexus.registry.NexusRecipes;
import com.morphengine.nexus.resource.ResourceTypes;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Nexus.MOD_ID)
public final class Nexus {

    public static final String MOD_ID = "nexus";

    public Nexus(final IEventBus modBus) {
        registerContent(modBus);
    }

    private void registerContent(final IEventBus modBus) {
        ResourceTypes.TYPES.register(modBus);
        UpgradeTypes.TYPES.register(modBus);
        NexusDataComponents.COMPONENTS.register(modBus);
        NexusBlocks.BLOCKS.register(modBus);
        NexusFluids.FLUID_TYPES.register(modBus);
        NexusFluids.FLUIDS.register(modBus);
        NexusItems.ITEMS.register(modBus);
        NexusFluids.bootstrap();
        NexusMetals.bootstrap();
        NexusMaterials.bootstrap();
        NexusRecipes.TYPES.register(modBus);
        NexusRecipes.SERIALIZERS.register(modBus);
        NexusBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modBus);
        NexusMenuTypes.MENU_TYPES.register(modBus);
        NexusCreativeTabs.CREATIVE_TABS.register(modBus);
    }
}
