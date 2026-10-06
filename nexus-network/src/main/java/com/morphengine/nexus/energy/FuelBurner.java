package com.morphengine.nexus.energy;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;

/**
 * Turns burning fuel into FE at a fixed rate per tick of burning. At a higher
 * {@linkplain #setSpeed speed} it burns several ticks of fuel each game tick,
 * so the same fuel gives the same FE sooner. Burning pauses, and no fuel is
 * wasted, while the target buffer has no room for a full tick of output.
 * Server thread only.
 */
public final class FuelBurner {

    private final long energyPerTick;
    private int speed = 1;
    private int efficiencyUpgrades;
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

    /**
     * @param ticksPerTick ticks of fuel burnt each game tick, must be positive
     */
    public void setSpeed(final int ticksPerTick) {
        if (ticksPerTick <= 0) {
            throw new IllegalArgumentException("speed must be positive: " + ticksPerTick);
        }
        this.speed = ticksPerTick;
    }

    /**
     * @param upgrades Efficiency Upgrades, from zero to {@link EfficiencyUpgrades#MAX_UPGRADES}: the same fuel
     *                 gives more FE
     */
    public void setEfficiencyUpgrades(final int upgrades) {
        EfficiencyUpgrades.yielded(0, upgrades);
        this.efficiencyUpgrades = upgrades;
    }

    /**
     * @return FE produced in a game tick of burning at the current speed and efficiency
     */
    public long outputPerTick() {
        return energyOf(speed);
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
        final long output = outputPerTick();
        return buffer.insert(output, Action.SIMULATE) == output;
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
     * Burns for one game tick, as many ticks of fuel as the speed says or what
     * is left of the piece, and puts the output into {@code buffer}.
     *
     * @return FE produced this tick; zero when not burning or when the buffer is full
     */
    public long tick(final EnergyBuffer buffer) {
        if (!isBurning() || !hasRoomIn(buffer)) {
            return 0;
        }
        final int burnt = Math.min(speed, burnTicksLeft);
        burnTicksLeft -= burnt;
        return buffer.insert(energyOf(burnt), Action.EXECUTE);
    }

    private long energyOf(final int burntTicks) {
        return EfficiencyUpgrades.yielded(energyPerTick * burntTicks, efficiencyUpgrades);
    }

    /**
     * Returns the burner to a saved state; values out of range are clamped.
     */
    public void restore(final int ticksLeft, final int ticksTotal) {
        burnTicksTotal = Math.max(0, ticksTotal);
        burnTicksLeft = Math.clamp(ticksLeft, 0, burnTicksTotal);
    }
}
