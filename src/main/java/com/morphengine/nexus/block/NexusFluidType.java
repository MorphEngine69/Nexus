package com.morphengine.nexus.block;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * A thick fluid of the mod that an entity moves in as it does in lava: slowly, sinking a little, able to swim up and to
 * climb out over an edge. The game moves an entity through the fluids it knows by itself; for any other fluid
 * {@link FluidType#move} must do it, and does nothing unless overridden, which leaves the entity stuck in place.
 */
public final class NexusFluidType extends FluidType {

    private static final float CONTROL = 0.02F;
    private static final double SHALLOW_HORIZONTAL_DRAG = 0.5;
    private static final double SHALLOW_VERTICAL_DRAG = 0.8;
    private static final double DEEP_DRAG = 0.5;
    private static final double SINK_DIVISOR = 4.0;
    private static final double CLIMB_SPEED = 0.3;
    private static final double CLIMB_REACH = 0.6;

    public NexusFluidType(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean move(
            final FluidState state, final LivingEntity entity, final Vec3 input, final double gravity) {
        final boolean isFalling = entity.getDeltaMovement().y <= 0.0;
        final double oldY = entity.getY();
        entity.moveRelative(CONTROL, input);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        if (entity.getFluidTypeHeight(this) <= entity.getFluidJumpThreshold()) {
            entity.setDeltaMovement(entity.getDeltaMovement()
                    .multiply(SHALLOW_HORIZONTAL_DRAG, SHALLOW_VERTICAL_DRAG, SHALLOW_HORIZONTAL_DRAG));
            entity.setDeltaMovement(
                    entity.getFluidFallingAdjustedMovement(gravity, isFalling, entity.getDeltaMovement()));
        } else {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(DEEP_DRAG));
        }
        if (gravity != 0.0) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -gravity / SINK_DIVISOR, 0.0));
        }
        climbOut(entity, oldY);
        return true;
    }

    private static void climbOut(final LivingEntity entity, final double oldY) {
        final Vec3 movement = entity.getDeltaMovement();
        if (entity.horizontalCollision
                && entity.isFree(movement.x, movement.y + CLIMB_REACH - entity.getY() + oldY, movement.z)) {
            entity.setDeltaMovement(movement.x, CLIMB_SPEED, movement.z);
        }
    }
}
