package com.morphengine.nexus.client.render;

import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Objects;

/**
 * A model whose bones are shown, hidden and scaled for each frame by its renderer, after the animations have set
 * theirs.
 */
public abstract class AdjustableGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    private @Nullable BoneAdjuster<T> adjuster;

    /**
     * Has the model call {@code newAdjuster} each time it is about to be drawn, with the bones of the model.
     */
    final void adjustWith(final BoneAdjuster<T> newAdjuster) {
        this.adjuster = Objects.requireNonNull(newAdjuster, "newAdjuster must not be null");
    }

    @Override
    public final void setCustomAnimations(
            final T animatable, final long instanceId, final @Nullable AnimationState<T> animationState) {
        if (adjuster != null) {
            adjuster.adjust(animatable, new Bones(this), animationState != null ? animationState.getPartialTick() : 0);
        }
    }

    /**
     * What a renderer does to the bones of its model before a frame.
     */
    @FunctionalInterface
    interface BoneAdjuster<T extends GeoAnimatable> {

        /**
         * @param partialTick how far into the current tick the frame is
         */
        void adjust(T animatable, Bones bones, float partialTick);
    }
}
