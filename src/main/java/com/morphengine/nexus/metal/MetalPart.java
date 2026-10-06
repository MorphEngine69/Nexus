package com.morphengine.nexus.metal;

/**
 * The basic items of a metal, apart from tools and armor.
 */
public enum MetalPart {

    RAW("raw"),
    INGOT("ingot"),
    NUGGET("nugget"),
    PLATE("plate"),
    DUST("dust");

    private final String name;

    MetalPart(final String name) {
        this.name = name;
    }

    /**
     * @return the id of the item of this part of {@code metal}: {@code steel_ingot}, but {@code raw_steel} as in
     *         vanilla
     */
    public String idFor(final MetalKind metal) {
        return this == RAW ? name + "_" + metal.id() : metal.id() + "_" + name;
    }
}
