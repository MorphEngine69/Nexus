package com.morphengine.nexus.client.render;

/**
 * The schedule of a bar of segments that shows something going on. At rest the segments a level lights are whole.
 * While it goes on they grow into their places one after another from the left, stay a while, then shrink away one
 * after another from the right, rest, and start again. The schedule is the same for every level and never changes
 * with it, so a change of the level, once a second, does not make the bar jump.
 */
final class SegmentSweep {

    /** For wide segments, as those of the bar of an Energy Cell. */
    static final SegmentSweep QUICK = new SegmentSweep(3, 5, 30, 10);
    /** For narrow segments, which look jerky when they grow as fast as wide ones. */
    static final SegmentSweep SLOW = new SegmentSweep(6, 10, 30, 10);

    private static final float SMOOTH_A = 3;
    private static final float SMOOTH_B = 2;

    private final float stepTicks;
    private final float growTicks;
    private final float holdTicks;
    private final float pauseTicks;

    private SegmentSweep(final int stepTicks, final int growTicks, final int holdTicks, final int pauseTicks) {
        this.stepTicks = stepTicks;
        this.growTicks = growTicks;
        this.holdTicks = holdTicks;
        this.pauseTicks = pauseTicks;
    }

    /**
     * @param index     the segment, counted from the left, from 0
     * @param level     how many segments are lit
     * @param segments  how many segments the bar has
     * @param sweeping  whether something is going on
     * @param ticks     the time, in ticks, never negative
     * @return how much of the segment shows, from 0 (none) to 1 (whole)
     */
    float fill(final int index, final int level, final int segments, final boolean sweeping, final float ticks) {
        if (index >= level) {
            return 0;
        }
        return sweeping ? sweepFill(index, segments, ticks) : 1;
    }

    private float sweepFill(final int index, final int segments, final float ticks) {
        final float sweep = (segments - 1) * stepTicks + growTicks;
        final float moment = ticks % (sweep + holdTicks + sweep + pauseTicks);
        if (moment < sweep) {
            return ease(clamp((moment - index * stepTicks) / growTicks));
        }
        if (moment < sweep + holdTicks) {
            return 1;
        }
        final float fromRight = (segments - 1 - index) * stepTicks;
        return 1 - ease(clamp((moment - sweep - holdTicks - fromRight) / growTicks));
    }

    private static float clamp(final float value) {
        return Math.max(0, Math.min(1, value));
    }

    private static float ease(final float value) {
        return value * value * (SMOOTH_A - SMOOTH_B * value);
    }
}
