package com.morphengine.nexus.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What a block entity drawn from an animated GeckoLib model needs to animate:
 * the cache of its animation state, and one controller that loops the animation
 * its block state asks for, blending from one to the next.
 */
final class BlockAnimation {

    private static final String CONTROLLER = "state";
    private static final int BLEND_TICKS = 10;

    private final AnimatableInstanceCache cache;
    private final Supplier<BlockState> state;
    private final Function<BlockState, RawAnimation> animationOf;
    private final DoubleSupplier phase;

    /**
     * @param state       the current block state of the owner
     * @param animationOf the animation to loop in a block state; asked for on the
     *                    client every frame, so it must not allocate
     * @param phase       how far through its animation the owner wants it to be, from 0 up to but not including 1, or
     *                    a negative number to let the animation run on its own
     */
    BlockAnimation(
            final GeoBlockEntity owner, final Supplier<BlockState> state,
            final Function<BlockState, RawAnimation> animationOf, final DoubleSupplier phase) {
        this.phase = Objects.requireNonNull(phase, "phase must not be null");
        this.cache = GeckoLibUtil.createInstanceCache(owner);
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.animationOf = Objects.requireNonNull(animationOf, "animationOf must not be null");
    }

    AnimatableInstanceCache cache() {
        return cache;
    }

    void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(CONTROLLER, BLEND_TICKS, test -> {
            final PlayState playing = test.setAndContinue(animationOf.apply(state.get()));
            final double wanted = phase.getAsDouble();
            final AnimationPoint point = test.controller().getCurrentAnimationPoint();
            if (wanted >= 0 && point != null) {
                test.controller().setAnimationTime(wanted * point.animation().length());
            }
            return playing;
        }));
    }
}
