package com.morphengine.nexus.client.render;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.item.NexusCrystalItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Draws the Nexus Crystal from its model and shines its glow mask over it, a little, as the crystal of the Nexus does.
 */
public final class CrystalItemRenderer extends GeoItemRenderer<NexusCrystalItem> {

    public CrystalItemRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID,
                "crystal/nexus_crystal")));
        addRenderLayer(new GlowWhenLitLayer<>(this, item -> true));
    }
}
