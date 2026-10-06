package com.morphengine.nexus.generator;

import com.morphengine.nexus.processing.MachinePhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What a generator gives off while it stands or works: the smoke and sparks of the coal, the pops of the lava,
 * the clouds of the steam, the glints round the star and the green sparks of the biofuel. Client side only, from
 * {@code animateTick}, which runs only now and then for a block, so the chances are high; none at all when the block
 * is dark. Positions are fractions of a block: across the front from its middle, up from its floor and out from its
 * front face.
 */
public final class GeneratorEffects {

    private static final double FRONT_OUT = 0.02;
    private static final double HALF_BLOCK = 0.5;
    private static final double SPARK_CHANCE = 0.3;
    private static final double CRACKLE_CHANCE = 0.1;
    private static final double COAL_MOUTH_HALF_WIDTH = 0.28;
    private static final double COAL_MOUTH_BOTTOM = 0.22;
    private static final double COAL_MOUTH_HEIGHT = 0.3;

    private static final double LAVA_WINDOW_HALF_WIDTH = 0.31;
    private static final double LAVA_WINDOW_BOTTOM = 0.28;
    private static final double LAVA_WINDOW_HEIGHT = 0.5;
    private static final double LAVA_POP_CHANCE = 0.25;
    private static final double LAVA_SMOKE_CHANCE = 0.1;
    private static final double LAVA_SOUND_CHANCE = 0.05;
    private static final float LAVA_SOUND_VOLUME = 0.5F;

    private static final double[] NOZZLES = {-0.2375, 0, 0.2375};
    private static final double NOZZLE_HEIGHT = 0.775;
    private static final double CLOUD_ACTIVE_CHANCE = 0.2;
    private static final double CLOUD_STANDBY_CHANCE = 0.05;
    private static final double CLOUD_SPEED = 0.03;
    private static final double CLOUD_RISE = 0.012;

    private static final double STAR_HALF_WIDTH = 0.18;
    private static final double STAR_BOTTOM = 0.3;
    private static final double STAR_HEIGHT = 0.45;
    private static final double STAR_DEPTH = -0.25;
    private static final double STAR_DEPTH_SPREAD = 0.15;
    private static final double STAR_ACTIVE_CHANCE = 0.6;
    private static final double STAR_STANDBY_CHANCE = 0.2;
    private static final double STAR_RISE = 0.01;

    private static final double[] TUBES = {-0.225, 0, 0.225};
    private static final double TUBE_TOP = 0.8;
    private static final double SPARK_ACTIVE_CHANCE = 0.5;
    private static final double SPARK_STANDBY_CHANCE = 0.1;
    private static final double SPARK_RISE = 0.01;

    private GeneratorEffects() {
    }

    /**
     * @param front the way the block faces, which its front, the face of the model, looks to
     */
    public static void animate(
            final GeneratorKind kind, final MachinePhase phase, final Direction front, final Level level,
            final BlockPos pos, final RandomSource random) {
        if (!phase.isLit()) {
            return;
        }
        final Frame frame = new Frame(pos, front);
        final boolean working = phase == MachinePhase.ACTIVE;
        switch (kind) {
            case COAL -> coal(frame, level, random, working);
            case LAVA -> lava(frame, level, random, working);
            case STEAM -> steam(frame, level, random, working);
            case NETHER_STAR -> star(frame, level, random, working);
            case BIOFUEL -> biofuel(frame, level, random, working);
        }
    }

    private static void coal(final Frame frame, final Level level, final RandomSource random, final boolean working) {
        if (!working) {
            return;
        }
        final Vec3 centre = Vec3.atCenterOf(frame.pos());
        if (random.nextDouble() < CRACKLE_CHANCE) {
            level.playLocalSound(centre.x, frame.pos().getY(), centre.z,
                    SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        final Vec3 mouth = frame.at(spread(random, COAL_MOUTH_HALF_WIDTH),
                COAL_MOUTH_BOTTOM + random.nextDouble() * COAL_MOUTH_HEIGHT, FRONT_OUT);
        level.addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 0, 0, 0);
        if (random.nextDouble() < SPARK_CHANCE) {
            level.addParticle(ParticleTypes.SMALL_FLAME, mouth.x, mouth.y, mouth.z, 0, 0, 0);
        }
    }

    private static void lava(final Frame frame, final Level level, final RandomSource random, final boolean working) {
        if (!working) {
            return;
        }
        final Vec3 surface = frame.at(spread(random, LAVA_WINDOW_HALF_WIDTH),
                LAVA_WINDOW_BOTTOM + random.nextDouble() * LAVA_WINDOW_HEIGHT, FRONT_OUT);
        if (random.nextDouble() < LAVA_POP_CHANCE) {
            level.addParticle(ParticleTypes.LAVA, surface.x, surface.y, surface.z, 0, 0, 0);
        }
        if (random.nextDouble() < LAVA_SMOKE_CHANCE) {
            level.addParticle(ParticleTypes.SMOKE, surface.x, surface.y, surface.z, 0, 0, 0);
        }
        if (random.nextDouble() < LAVA_SOUND_CHANCE) {
            level.playLocalSound(surface.x, surface.y, surface.z, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                    LAVA_SOUND_VOLUME, 1.0F, false);
        }
    }

    private static void steam(final Frame frame, final Level level, final RandomSource random, final boolean working) {
        if (working) {
            for (double nozzle : NOZZLES) {
                if (random.nextDouble() < CLOUD_ACTIVE_CHANCE) {
                    cloud(frame, level, nozzle);
                }
            }
        } else if (random.nextDouble() < CLOUD_STANDBY_CHANCE) {
            cloud(frame, level, NOZZLES[random.nextInt(NOZZLES.length)]);
        }
    }

    private static void cloud(final Frame frame, final Level level, final double nozzle) {
        final Vec3 mouth = frame.at(nozzle, NOZZLE_HEIGHT, FRONT_OUT);
        level.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, frame.front().getStepX() * CLOUD_SPEED,
                CLOUD_RISE, frame.front().getStepZ() * CLOUD_SPEED);
    }

    private static void star(final Frame frame, final Level level, final RandomSource random, final boolean working) {
        if (random.nextDouble() >= (working ? STAR_ACTIVE_CHANCE : STAR_STANDBY_CHANCE)) {
            return;
        }
        final Vec3 point = frame.at(spread(random, STAR_HALF_WIDTH), STAR_BOTTOM + random.nextDouble() * STAR_HEIGHT,
                STAR_DEPTH + spread(random, STAR_DEPTH_SPREAD));
        level.addParticle(ParticleTypes.END_ROD, point.x, point.y, point.z, 0, STAR_RISE, 0);
    }

    private static void biofuel(
            final Frame frame, final Level level, final RandomSource random, final boolean working) {
        if (random.nextDouble() >= (working ? SPARK_ACTIVE_CHANCE : SPARK_STANDBY_CHANCE)) {
            return;
        }
        final Vec3 top = frame.at(TUBES[random.nextInt(TUBES.length)], TUBE_TOP, FRONT_OUT);
        final ParticleOptions spark = ParticleTypes.COMPOSTER;
        level.addParticle(spark, top.x, top.y, top.z, 0, SPARK_RISE, 0);
    }

    private static double spread(final RandomSource random, final double halfWidth) {
        return (random.nextDouble() * 2 - 1) * halfWidth;
    }

    /**
     * Where a point of the front of a block is in the world.
     */
    private record Frame(BlockPos pos, Direction front) {

        Vec3 at(final double across, final double height, final double out) {
            final Direction right = front.getCounterClockWise();
            final Vec3 centre = Vec3.atCenterOf(pos);
            final double reach = HALF_BLOCK + out;
            return new Vec3(centre.x + front.getStepX() * reach + right.getStepX() * across,
                    pos.getY() + height,
                    centre.z + front.getStepZ() * reach + right.getStepZ() * across);
        }
    }
}
