package com.morphengine.nexus.metal;

/**
 * The metals of vanilla that the machines of the mod grind into dust and press into plates.
 */
public enum VanillaMetal {

    IRON("iron"),
    GOLD("gold"),
    COPPER("copper");

    private final String id;

    VanillaMetal(final String id) {
        this.id = id;
    }

    /**
     * @return the name in the ids of the dust and the plate, such as {@code iron}
     */
    public String id() {
        return id;
    }
}
