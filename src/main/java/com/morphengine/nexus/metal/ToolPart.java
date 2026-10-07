package com.morphengine.nexus.metal;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;

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
            case AXE -> new AxeItem(
                    metal.tools(), metal.handling().axeDamage(), metal.handling().axeSpeed(), properties);
            case SHOVEL -> new ShovelItem(metal.tools(), SHOVEL_DAMAGE, SHOVEL_SPEED, properties);
            case HOE -> new HoeItem(
                    metal.tools(), metal.handling().hoeDamage(), metal.handling().hoeSpeed(), properties);
            case SWORD -> new Item(properties.sword(metal.tools(), SWORD_DAMAGE, SWORD_SPEED));
        };
    }
}
