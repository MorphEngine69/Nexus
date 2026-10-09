package com.morphengine.nexus.metal;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The armor a metal makes: what it protects against, how long it lasts and what repairs it.
 *
 * @param durabilityFactor multiplied by the base durability of each piece
 * @param defense          protection of each piece
 * @param enchantability   how well the armor takes enchantments
 * @param equipSound       the sound of putting it on
 * @param toughness        toughness of each piece
 * @param knockbackResistance knockback resistance of each piece
 * @param repairItems      the items that repair the armor
 */
public record MetalArmor(int durabilityFactor, Map<ArmorItem.Type, Integer> defense, int enchantability,
                         Holder<SoundEvent> equipSound, float toughness, float knockbackResistance,
                         TagKey<Item> repairItems) {

    public MetalArmor {
        Objects.requireNonNull(defense, "defense");
        Objects.requireNonNull(equipSound, "equipSound");
        Objects.requireNonNull(repairItems, "repairItems");
        defense = new EnumMap<>(defense);
    }

    /**
     * @return the material of the game for the armor of metal {@code id}, whose model layers come from
     *         {@code textures/models/armor/<id>_layer_1.png} and {@code _layer_2.png}
     */
    public ArmorMaterial material(final String id) {
        final List<ArmorMaterial.Layer> layers = List.of(
                new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, id)));
        return new ArmorMaterial(defense, enchantability, equipSound, () -> Ingredient.of(repairItems), layers,
                toughness, knockbackResistance);
    }
}
