package com.morphengine.nexus.metal;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

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
    private static final int SWORD_DAMAGE = 3;
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
        final MetalTier tier = metal.tools();
        return switch (this) {
            case PICKAXE -> new PickaxeItem(tier,
                    properties.attributes(PickaxeItem.createAttributes(tier, PICKAXE_DAMAGE, PICKAXE_SPEED)));
            case AXE -> new AxeItem(tier, properties.attributes(AxeItem.createAttributes(
                    tier, metal.handling().axeDamage(), metal.handling().axeSpeed())));
            case SHOVEL -> new ShovelItem(tier,
                    properties.attributes(ShovelItem.createAttributes(tier, SHOVEL_DAMAGE, SHOVEL_SPEED)));
            case HOE -> new HoeItem(tier, properties.attributes(HoeItem.createAttributes(
                    tier, metal.handling().hoeDamage(), metal.handling().hoeSpeed())));
            case SWORD -> new SwordItem(tier,
                    properties.attributes(SwordItem.createAttributes(tier, SWORD_DAMAGE, SWORD_SPEED)));
        };
    }
}
