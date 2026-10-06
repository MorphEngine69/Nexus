package com.morphengine.nexus.client.render;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.item.NexusCrystalItem;
import net.minecraft.resources.Identifier;

/**
 * Draws the Nexus Crystal from its model and shines its glow mask over it, a little, as the crystal of the Nexus does.
 */
public final class CrystalItemRenderer extends GeoItemRenderer<NexusCrystalItem> {

    public CrystalItemRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "crystal/nexus_crystal")));
        withRenderLayer(
                new AutoGlowingGeoLayer<NexusCrystalItem, GeoItemRenderer.RenderData, GeoRenderState>(this));
    }
}
