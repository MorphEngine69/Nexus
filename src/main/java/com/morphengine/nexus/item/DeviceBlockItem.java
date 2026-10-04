package com.morphengine.nexus.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.client.render.DeviceItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The item of a device drawn from its GeckoLib model, held still: it shows one pose of the model, {@code idle} unless
 * its {@link Look} says another, so that the inventory and the creative tabs, with many of these on screen, do not
 * animate every one of them.
 */
public final class DeviceBlockItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Look look;
    private final RawAnimation still;

    public DeviceBlockItem(final Block block, final Properties properties, final Look look) {
        super(block, properties);
        this.look = Objects.requireNonNull(look, "look must not be null");
        this.still = RawAnimation.begin().thenLoop(look.animation());
    }

    @Override
    public void createGeoRenderer(final Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private @Nullable DeviceItemRenderer renderer;

            @Override
            public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new DeviceItemRenderer(look);
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("state", state -> state.setAndContinue(still)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /**
     * How an item looks: which model, where in the slot, in which color and in which pose of the model.
     *
     * @param asset                  the name of the model of the device
     * @param shiftTowardFrontPixels how far the model is moved toward its front, in pixels, so that a model that stands
     *                               against the back of its block is drawn in the middle
     * @param color                  the color of the texture, for a device that has a color of its own
     * @param animation              the pose of the model to show, the name of one of its animations
     */
    public record Look(String asset, float shiftTowardFrontPixels, DyeColor color, String animation) {

        private static final String STILL = "idle";

        public Look {
            Objects.requireNonNull(asset, "asset must not be null");
            Objects.requireNonNull(color, "color must not be null");
            Objects.requireNonNull(animation, "animation must not be null");
        }

        public static Look of(final String asset) {
            return new Look(asset, 0, NetworkColoring.UNCONNECTED, STILL);
        }

        public Look shifted(final float pixels) {
            return new Look(asset, pixels, color, animation);
        }

        public Look colored(final DyeColor newColor) {
            return new Look(asset, shiftTowardFrontPixels, newColor, animation);
        }

        public Look posed(final String newAnimation) {
            return new Look(asset, shiftTowardFrontPixels, color, newAnimation);
        }
    }
}
