package com.morphengine.nexus.automation;

/**
 * Lets requests through no closer together than a gap of game ticks, so that one asker cannot keep the server busy
 * with work it has to do for each. A request that is turned away does not move the gap. Server thread only.
 */
public final class RequestThrottle {

    private final int gapTicks;
    private boolean hasAdmitted;
    private long lastAdmittedAt;

    /**
     * @param gapTicks ticks that must pass after a request that was let through before the next one is
     */
    public RequestThrottle(final int gapTicks) {
        if (gapTicks < 0) {
            throw new IllegalArgumentException("gapTicks must not be negative: " + gapTicks);
        }
        this.gapTicks = gapTicks;
    }

    /**
     * @param gameTime the tick the request came in
     * @return whether it may go ahead; if so, the gap runs from now
     */
    public boolean tryAdmit(final long gameTime) {
        if (hasAdmitted && gameTime - lastAdmittedAt < gapTicks) {
            return false;
        }
        hasAdmitted = true;
        lastAdmittedAt = gameTime;
        return true;
    }
}
