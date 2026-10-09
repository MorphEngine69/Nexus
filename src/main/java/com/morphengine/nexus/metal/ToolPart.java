package com.morphengine.nexus.metal;

import net.minecraft.world.item.Item;

/**
 * The tools of a metal.
 */
public enum ToolPart {

    PICKAXE("pickaxe"),
    AXE("axe"),
    SHOVEL("shovel"),
    HOE("hoe"),
    SWORD("sword");

    private static final float PICKAXE_DAMAGE = 1.0F;
    private static final float PICKAXE_SPEED = -2.8F;
    private static final float SHOVEL_DAMAGE = 1.5F;
    private static final float SHOVEL_SPEED = -3.0F;
    private static final float SWORD_DAMAGE = 3.0F;
    private static final float SWORD_SPEED = -2.4F;

    private final String name;

    ToolPart(final String name) {
        this.name = name;
    }

    /**
     * @return the id of the item of this tool of {@code metal}, such as {@code steel_pickaxe}
     */
    public String idFor(final MetalKind metal) {
        return metal.id() + "_" + name;
    }

    public Item create(final MetalKind metal, final Item.Properties properties) {
        return switch (this) {
            case PICKAXE -> new Item(properties.pickaxe(metal.tools(), PICKAXE_DAMAGE, PICKAXE_SPEED));
            case AXE -> new Item(properties.axe(
                    metal.tools(), metal.handling().axeDamage(), metal.handling().axeSpeed()));
            case SHOVEL -> new Item(properties.shovel(metal.tools(), SHOVEL_DAMAGE, SHOVEL_SPEED));
            case HOE -> new Item(properties.hoe(
                    metal.tools(), metal.handling().hoeDamage(), metal.handling().hoeSpeed()));
            case SWORD -> new Item(properties.sword(metal.tools(), SWORD_DAMAGE, SWORD_SPEED));
        };
    }
}
