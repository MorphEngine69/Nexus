package com.morphengine.nexus.item;

import com.morphengine.nexus.client.render.CrystalItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;
import java.util.function.Consumer;

/**
 * The Nexus Crystal, drawn from its GeckoLib model: still in a slot of the inventory and in the creative tabs, where
 * many of them are on screen at once, and slowly turning, with a light glow, in a hand and on the ground.
 */
public final class NexusCrystalItem extends Item implements GeoItem {

    private static final RawAnimation STILL = RawAnimation.begin().thenLoop("still");
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private static final Set<ItemDisplayContext> STILL_IN =
            Set.of(ItemDisplayContext.GUI, ItemDisplayContext.NONE, ItemDisplayContext.FIXED);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public NexusCrystalItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void createGeoRenderer(final Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private @Nullable CrystalItemRenderer renderer;

            @Override
            public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new CrystalItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "crystal", state -> state.setAndContinue(
                STILL_IN.contains(perspectiveOf(state))
                        ? STILL : SPIN)));
    }

    private static ItemDisplayContext perspectiveOf(final AnimationState<NexusCrystalItem> state) {
        final ItemDisplayContext perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        return perspective != null ? perspective : ItemDisplayContext.NONE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
