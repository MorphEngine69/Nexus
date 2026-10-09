package com.morphengine.nexus.block.entity;

import software.bernie.geckolib.util.RenderUtil;

/**
 * The time on the client in ticks, with the part of the tick that has passed. Client side only: nothing here is
 * loaded before the client asks for an animation.
 */
final class CycleClock {

    private CycleClock() {
    }

    static double now() {
        return RenderUtil.getCurrentTick();
    }
}
