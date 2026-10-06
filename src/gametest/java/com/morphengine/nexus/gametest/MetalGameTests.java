package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.metal.MetalKind;
import com.morphengine.nexus.metal.MetalPart;
import com.morphengine.nexus.metal.ToolPart;
import com.morphengine.nexus.processing.AlloyInput;
import com.morphengine.nexus.registry.NexusMetals;
import com.morphengine.nexus.registry.NexusRecipes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The metals of the mod: what their tools can mine, what repairs them, whether they burn, and that their recipes and
 * ores load.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class MetalGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 20;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("steel_ore_needs_a_stone_tool", MetalGameTests::steelOreNeedsStone),
            Map.entry("cobalt_and_mithril_ore_need_an_iron_tool", MetalGameTests::cobaltAndMithrilNeedIron),
            Map.entry("hellsteel_ore_needs_a_diamond_tool", MetalGameTests::hellsteelNeedsDiamond),
            Map.entry("a_metal_is_repaired_with_its_own_ingot", MetalGameTests::repairedWithOwnIngot),
            Map.entry("only_hellsteel_resists_fire", MetalGameTests::onlyHellsteelResistsFire),
            Map.entry("every_armor_piece_goes_on_its_own_slot", MetalGameTests::armorSlots),
            Map.entry("every_recipe_of_every_metal_is_loaded", MetalGameTests::recipesAreLoaded),
            Map.entry("every_ore_is_placed_in_its_biomes", MetalGameTests::oresArePlaced),
            Map.entry("each_alloy_is_made_of_its_three_ingots", MetalGameTests::alloysAreMade));

    private MetalGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "metals"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static ItemStack pickaxe(final MetalKind metal) {
        return new ItemStack(NexusMetals.of(metal).tools().get(ToolPart.PICKAXE).get());
    }

    private static BlockState ore(final MetalKind metal) {
        return NexusMetals.of(metal).ores().getFirst().get().defaultBlockState();
    }

    private static void assertMines(final GameTestHelper helper, final ItemStack tool, final BlockState block,
                                    final boolean expected, final String what) {
        helper.assertValueEqual(tool.isCorrectToolForDrops(block), expected, Component.literal(what));
    }

    private static void steelOreNeedsStone(final GameTestHelper helper) {
        assertMines(helper, new ItemStack(Items.WOODEN_PICKAXE), ore(MetalKind.STEEL), false, "a wooden pickaxe");
        assertMines(helper, new ItemStack(Items.STONE_PICKAXE), ore(MetalKind.STEEL), true, "a stone pickaxe");
        helper.succeed();
    }

    private static void cobaltAndMithrilNeedIron(final GameTestHelper helper) {
        for (MetalKind metal : List.of(MetalKind.COBALT, MetalKind.MITHRIL)) {
            assertMines(helper, new ItemStack(Items.STONE_PICKAXE), ore(metal), false,
                    "a stone pickaxe on " + metal.id());
            assertMines(helper, pickaxe(MetalKind.STEEL), ore(metal), true, "a steel pickaxe on " + metal.id());
        }
        helper.succeed();
    }

    private static void hellsteelNeedsDiamond(final GameTestHelper helper) {
        final BlockState ore = ore(MetalKind.HELLSTEEL);
        assertMines(helper, pickaxe(MetalKind.STEEL), ore, false, "a steel pickaxe");
        assertMines(helper, new ItemStack(Items.IRON_PICKAXE), ore, false, "an iron pickaxe");
        assertMines(helper, pickaxe(MetalKind.COBALT), ore, true, "a cobalt pickaxe");
        assertMines(helper, pickaxe(MetalKind.MITHRIL), ore, true, "a mithril pickaxe");
        assertMines(helper, pickaxe(MetalKind.HELLSTEEL), ore, true, "a hellsteel pickaxe");
        helper.succeed();
    }

    private static void repairedWithOwnIngot(final GameTestHelper helper) {
        for (MetalKind metal : MetalKind.ALL) {
            for (MetalKind other : MetalKind.ALL) {
                final ItemStack ingot = new ItemStack(NexusMetals.of(other).part(MetalPart.INGOT).get());
                helper.assertValueEqual(pickaxe(metal).isValidRepairItem(ingot), metal == other,
                        Component.literal(metal.id() + " tool repaired with " + other.id() + " ingot"));
            }
        }
        helper.succeed();
    }

    private static void onlyHellsteelResistsFire(final GameTestHelper helper) {
        for (MetalKind metal : MetalKind.ALL) {
            helper.assertValueEqual(pickaxe(metal).has(DataComponents.DAMAGE_RESISTANT), metal == MetalKind.HELLSTEEL,
                    Component.literal("fire resistance of " + metal.id()));
        }
        helper.succeed();
    }

    private static void armorSlots(final GameTestHelper helper) {
        final Map<ArmorType, EquipmentSlot> slots = Map.of(ArmorType.HELMET, EquipmentSlot.HEAD,
                ArmorType.CHESTPLATE, EquipmentSlot.CHEST, ArmorType.LEGGINGS, EquipmentSlot.LEGS,
                ArmorType.BOOTS, EquipmentSlot.FEET);
        for (MetalKind metal : MetalKind.ALL) {
            slots.forEach((piece, slot) -> {
                final ItemStack stack = new ItemStack(NexusMetals.of(metal).armor().get(piece).get());
                helper.assertValueEqual(stack.get(DataComponents.EQUIPPABLE).slot(), slot,
                        Component.literal("slot of " + metal.id() + " " + piece.getName()));
            });
        }
        helper.succeed();
    }

    private static void recipesAreLoaded(final GameTestHelper helper) {
        for (MetalKind metal : MetalKind.ALL) {
            for (String name : recipeNames(metal)) {
                final ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name));
                helper.assertTrue(helper.getLevel().getServer().getRecipeManager().recipeMap().byKey(key) != null,
                        Component.literal("recipe " + name + " is not loaded"));
            }
        }
        helper.succeed();
    }

    private static void alloysAreMade(final GameTestHelper helper) {
        final Map<String, List<ItemStack>> alloys = Map.of(
                "voltsteel_ingot", List.of(stackOf("steel_ingot"), stackOf("hellsteel_ingot"),
                        new ItemStack(Items.BLAZE_POWDER)),
                "lumen_ingot", List.of(stackOf("cobalt_ingot"), new ItemStack(Items.QUARTZ),
                        new ItemStack(Items.LAPIS_LAZULI)),
                "aether_ingot", List.of(stackOf("mithril_ingot"), stackOf("cobalt_ingot"),
                        new ItemStack(Items.DIAMOND)));
        final var recipes = helper.getLevel().getServer().getRecipeManager().recipeMap()
                .byType(NexusRecipes.ALLOYING.get());
        alloys.forEach((result, inputs) -> {
            final var recipe = recipes.stream().map(holder -> holder.value())
                    .filter(candidate -> candidate.resultStack().is(stackOf(result).getItem())).findFirst();
            helper.assertTrue(recipe.isPresent(), Component.literal("no alloy recipe for " + result));
            helper.assertTrue(recipe.get().matches(new AlloyInput(inputs), helper.getLevel()),
                    Component.literal(result + " is not made of its ingots"));
        });
        helper.succeed();
    }

    private static ItemStack stackOf(final String name) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name)));
    }

    private static List<String> recipeNames(final MetalKind metal) {
        final String id = metal.id();
        final List<String> names = new java.util.ArrayList<>(List.of(id + "_nugget", id + "_ingot_from_nuggets",
                id + "_block", id + "_ingot_from_block", "crushing_raw_" + id, "pulverizing_" + id + "_ingot",
                "compressing_" + id + "_ingot", id + "_ingot_from_smelting_raw_" + id,
                id + "_ingot_from_blasting_" + id + "_dust"));
        for (ToolPart tool : ToolPart.values()) {
            names.add(tool.idFor(metal));
        }
        for (ArmorType piece : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
            names.add(id + "_" + piece.getName());
        }
        return names;
    }

    private static void oresArePlaced(final GameTestHelper helper) {
        final Map<String, String> featureByBiome = Map.of(
                "plains", "ore_steel_upper", "badlands", "ore_cobalt_extra", "dripstone_caves", "ore_mithril",
                "nether_wastes", "ore_hellsteel_large", "deep_dark", "ore_nexus");
        featureByBiome.forEach((biomeName, featureName) -> {
            final Holder<Biome> biome = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME)
                    .getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(biomeName)));
            final Identifier feature = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, featureName);
            final boolean placed = biome.value().getGenerationSettings().features().stream()
                    .flatMap(HolderSet::stream)
                    .anyMatch(holder -> holder.is(ResourceKey.create(Registries.PLACED_FEATURE, feature)));
            helper.assertTrue(placed, Component.literal(featureName + " is not placed in " + biomeName));
        });
        helper.succeed();
    }
}
