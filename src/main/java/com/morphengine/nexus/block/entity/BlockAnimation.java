package com.morphengine.nexus.block.entity;

import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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

    private final GeoBlockEntity owner;
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
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.phase = Objects.requireNonNull(phase, "phase must not be null");
        this.cache = GeckoLibUtil.createInstanceCache(owner);
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.animationOf = Objects.requireNonNull(animationOf, "animationOf must not be null");
    }

    AnimatableInstanceCache cache() {
        return cache;
    }

    void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new PhasedController<>(owner, CONTROLLER, BLEND_TICKS, test -> {
            final PlayState playing = test.setAndContinue(animationOf.apply(state.get()));
            final double wanted = phase.getAsDouble();
            if (wanted >= 0 && test.getController() instanceof PhasedController<GeoBlockEntity> controller) {
                controller.seekToPhase(wanted, test.getAnimationTick());
            }
            return playing;
        }));
    }

    /**
     * A controller that can be told how far through its animation it is. The new place takes hold from the next frame,
     * which is soon enough for an animation that follows work that lasts a good many ticks.
     */
    private static final class PhasedController<T extends GeoBlockEntity> extends AnimationController<T> {

        PhasedController(
                final T animatable, final String name, final int transitionTicks,
                final AnimationStateHandler<T> handler) {
            super(animatable, name, transitionTicks, handler);
        }

        void seekToPhase(final double fraction, final double seekTime) {
            if (getAnimationState() != State.RUNNING || currentAnimation == null) {
                return;
            }
            final double speed = getAnimationSpeed();
            if (speed > 0) {
                tickOffset = seekTime - fraction * currentAnimation.animation().length() / speed;
            }
        }
    }
}
