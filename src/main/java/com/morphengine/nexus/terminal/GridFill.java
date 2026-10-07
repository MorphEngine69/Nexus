package com.morphengine.nexus.terminal;

/**
 * How much of a recipe a recipe viewer asks to lay out on a crafting grid.
 */
public enum GridFill {

    /** One of each ingredient: enough for a single craft. */
    ONE_CRAFT,

    /**
     * As many crafts as the ingredients in the inventory and the network last
     * for, up to a stack of the smallest stacking one; what the viewer's
     * shift-click asks for.
     */
    MOST_CRAFTS
}
