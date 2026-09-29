package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;

/**
 * Turns burning fuel into FE at a fixed rate. Burning pauses, and no fuel is
 * wasted, while the target buffer has no room for a full tick of output.
 * Server thread only.
 */
public final class FuelBurner {

    private final long energyPerTick;
    private int burnTicksLeft;
    private int burnTicksTotal;

    /**
     * @param energyPerTick FE produced per tick of burning, must be positive
     */
    public FuelBurner(final long energyPerTick) {
        if (energyPerTick <= 0) {
            throw new IllegalArgumentException("energyPerTick must be positive: " + energyPerTick);
        }
        this.energyPerTick = energyPerTick;
    }

    public long energyPerTick() {
        return energyPerTick;
    }

    public boolean isBurning() {
        return burnTicksLeft > 0;
    }

    public int burnTicksLeft() {
        return burnTicksLeft;
    }

    public int burnTicksTotal() {
        return burnTicksTotal;
    }

    /**
     * @return whether {@code buffer} can take a full tick of output, so that a
     *         new piece of fuel is worth lighting
     */
    public boolean hasRoomIn(final EnergyBuffer buffer) {
        return buffer.insert(energyPerTick, Action.SIMULATE) == energyPerTick;
    }

    /**
     * Starts burning one piece of fuel.
     *
     * @param burnTicks how long the fuel burns, must be positive
     * @throws IllegalStateException if fuel is already burning
     */
    public void ignite(final int burnTicks) {
        if (burnTicks <= 0) {
            throw new IllegalArgumentException("burnTicks must be positive: " + burnTicks);
        }
        if (isBurning()) {
            throw new IllegalStateException("fuel is still burning, " + burnTicksLeft + " ticks left");
        }
        burnTicksLeft = burnTicks;
        burnTicksTotal = burnTicks;
    }

    /**
     * Burns for one tick and puts the output into {@code buffer}.
     *
     * @return FE produced this tick; zero when not burning or when the buffer is full
     */
    public long tick(final EnergyBuffer buffer) {
        if (!isBurning() || !hasRoomIn(buffer)) {
            return 0;
        }
        burnTicksLeft--;
        return buffer.insert(energyPerTick, Action.EXECUTE);
    }

    /**
     * Returns the burner to a saved state; values out of range are clamped.
     */
    public void restore(final int ticksLeft, final int ticksTotal) {
        burnTicksTotal = Math.max(0, ticksTotal);
        burnTicksLeft = Math.clamp(ticksLeft, 0, burnTicksTotal);
    }
}
