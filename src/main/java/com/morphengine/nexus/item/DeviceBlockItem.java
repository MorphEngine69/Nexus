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
import com.morphengine.nexus.registry.NexusDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The item of a device drawn from its GeckoLib model, held still: it shows one pose of the model, {@code idle} unless
 * its {@link Look} says another, so that the inventory and the creative tabs, with many of these on screen, do not
 * animate every one of them.
 */
public final class DeviceBlockItem extends BlockItem implements GeoItem {

    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance();

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Look look;
    private final RawAnimation still;

    public DeviceBlockItem(final Block block, final Properties properties, final Look look) {
        super(block, properties);
        this.look = Objects.requireNonNull(look, "look must not be null");
        this.still = RawAnimation.begin().thenLoop(look.animation());
    }

    /**
     * Tells what a machine or a generator that was taken down still holds: its FE and each fluid in its tanks.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        final long energy = stack.getOrDefault(NexusDataComponents.STORED_ENERGY.get(), 0L);
        if (energy > 0) {
            builder.accept(Component.translatable("tooltip.nexus.stored_energy", NUMBERS.format(energy))
                    .withStyle(ChatFormatting.GRAY));
        }
        for (FluidStack fluid : stack.getOrDefault(NexusDataComponents.STORED_FLUIDS.get(),
                StoredFluids.EMPTY).tanks()) {
            if (!fluid.isEmpty()) {
                builder.accept(Component.translatable("tooltip.nexus.stored_fluid", fluid.getHoverName(),
                        NUMBERS.format(fluid.getAmount())).withStyle(ChatFormatting.GRAY));
            }
        }
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
     * @param slotColor              the color of the texture in an inventory slot, where it can differ from the one
     *                               the device has in the hand and in the world
     * @param animation              the pose of the model to show, the name of one of its animations
     * @param hiddenBones            the bones of the model that the item does not show
     * @param textureSuffix          what follows the color in the name of the texture, such as {@code _active},
     *                               for a model that has a texture for each state
     */
    public record Look(
            String asset, float shiftTowardFrontPixels, DyeColor color, DyeColor slotColor, String animation,
            List<String> hiddenBones, String textureSuffix) {

        private static final String STILL = "idle";

        public Look {
            Objects.requireNonNull(asset, "asset must not be null");
            Objects.requireNonNull(color, "color must not be null");
            Objects.requireNonNull(slotColor, "slotColor must not be null");
            Objects.requireNonNull(animation, "animation must not be null");
            Objects.requireNonNull(textureSuffix, "textureSuffix must not be null");
            hiddenBones = List.copyOf(hiddenBones);
        }

        public static Look of(final String asset) {
            return new Look(asset, 0, NetworkColoring.UNCONNECTED, NetworkColoring.UNCONNECTED, STILL, List.of(), "");
        }

        public Look shifted(final float pixels) {
            return new Look(asset, pixels, color, slotColor, animation, hiddenBones, textureSuffix);
        }

        public Look colored(final DyeColor newColor) {
            return new Look(asset, shiftTowardFrontPixels, newColor, newColor, animation, hiddenBones, textureSuffix);
        }

        /**
         * @return the look with another color in an inventory slot only: the device looks the same in the hand
         */
        public Look inSlot(final DyeColor newSlotColor) {
            return new Look(asset, shiftTowardFrontPixels, color, newSlotColor, animation, hiddenBones,
                    textureSuffix);
        }

        public Look posed(final String newAnimation) {
            return new Look(asset, shiftTowardFrontPixels, color, slotColor, newAnimation, hiddenBones,
                    textureSuffix);
        }

        public Look withTextureSuffix(final String suffix) {
            return new Look(asset, shiftTowardFrontPixels, color, slotColor, animation, hiddenBones, suffix);
        }

        public Look hiding(final List<String> bones) {
            return new Look(asset, shiftTowardFrontPixels, color, slotColor, animation, bones, textureSuffix);
        }
    }
}
