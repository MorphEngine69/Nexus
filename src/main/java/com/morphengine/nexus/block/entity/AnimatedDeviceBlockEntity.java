package com.morphengine.nexus.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * A network device with a panel that is drawn from an animated GeckoLib model,
 * like {@link AnimatedBlockEntity}: one animation looping at a time, the one
 * its block state asks for. Designed for extension.
 */
public abstract class AnimatedDeviceBlockEntity extends NetworkDeviceBlockEntity implements GeoBlockEntity {

    private final BlockAnimation animation;

    /**
     * @param animationOf the animation to loop in a block state; asked for on the
     *                    client every frame, so it must not allocate
     */
    protected AnimatedDeviceBlockEntity(
            final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
            final Function<BlockState, RawAnimation> animationOf) {
        super(type, pos, state);
        this.animation = new BlockAnimation(this, this::getBlockState, animationOf);
    }

    @Override
    public final void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        animation.registerControllers(controllers);
    }

    @Override
    public final AnimatableInstanceCache getAnimatableInstanceCache() {
        return animation.cache();
    }
}
