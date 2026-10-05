package com.morphengine.nexus.block.entity;

import com.geckolib.util.ClientUtil;

/**
 * The time on the client in ticks, with the part of the tick that has passed. Client side only: nothing here is
 * loaded before the client asks for an animation.
 */
final class CycleClock {

    private CycleClock() {
    }

    static double now() {
        return ClientUtil.getCurrentTick();
    }
}
