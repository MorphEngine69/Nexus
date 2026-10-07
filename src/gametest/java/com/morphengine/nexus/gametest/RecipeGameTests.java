package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * What the player can craft: every block and item of the mod that is not found in the world or made by a machine has a
 * shaped crafting recipe; a recipe with an ingredient that does not exist is not loaded, so it would be missing.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class RecipeGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 100;

    private static final List<String> TIERS = List.of("basic", "advanced", "superior", "quantum");
    private static final List<String> DEVICES = List.of(
            "energy_furnace", "crusher", "pulverizer", "compressor", "alloy_smelter", "extractor");
    private static final List<String> SIZES = List.of("1k", "4k", "16k", "64k", "256k", "512k");
    private static final List<String> CELL_LINES = List.of("item_vault_cell", "fluid_vault_cell", "energy_vault_cell");
    private static final List<String> CRAFTED = List.of(
            "core", "upgrade_blank", "machine_casing", "battery", "screen", "antenna", "cell_housing", "polymer",
            "nexus", "cable", "wrench", "terminal", "crafting_terminal", "basic_energy_cell", "coal_generator",
            "lava_generator", "steam_generator", "biofuel_generator", "nether_star_generator", "storage_vault",
            "external_vault", "puller", "pusher", "placer", "remover", "blueprint", "blueprint_terminal", "assembler",
            "crafting_monitor", "basic_energy_furnace", "basic_crusher", "basic_pulverizer", "basic_compressor",
            "basic_alloy_smelter", "basic_extractor", "network_card", "network_transmitter", "network_receiver",
            "nexus_link", "nexus_terminal", "nexus_analyser", "speed_upgrade", "stack_upgrade", "regulator_upgrade",
            "capacity_upgrade",
            "range_upgrade", "fortune_upgrade", "silk_touch_upgrade", "autocrafting_upgrade", "chunk_loader_upgrade",
            "dimension_upgrade", "efficiency_upgrade", "buffer_upgrade", "void_upgrade", "advanced_tier_upgrade",
            "superior_tier_upgrade", "quantum_tier_upgrade", "advanced_energy_cell", "superior_energy_cell",
            "quantum_energy_cell");

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "every_block_and_item_that_is_crafted_has_a_recipe", RecipeGameTests::everythingCraftedHasARecipe,
            "every_cell_and_cell_part_of_every_size_has_a_recipe", RecipeGameTests::everyCellSizeHasARecipe,
            "polymer_comes_four_from_slime_and_two_from_sugar", RecipeGameTests::polymerYields,
            "tiered_devices_are_crafted_one_tier_at_a_time", RecipeGameTests::tiersAreCraftedInOrder,
            "the_first_screen_and_storage_need_only_steel_and_stock_items", RecipeGameTests::earlyStorageAndScreen);

    private RecipeGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "recipes"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void everythingCraftedHasARecipe(final GameTestHelper helper) {
        assertCrafted(helper, CRAFTED);
        helper.succeed();
    }

    private static void everyCellSizeHasARecipe(final GameTestHelper helper) {
        final List<String> names = new ArrayList<>();
        for (String size : SIZES) {
            names.add("cell_part_" + size);
            CELL_LINES.forEach(line -> names.add(line + "_" + size));
        }
        assertCrafted(helper, names);
        helper.succeed();
    }

    private static void polymerYields(final GameTestHelper helper) {
        final Identifier polymer = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "polymer");
        final Set<Integer> counts = new HashSet<>();
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().recipeMap().values()) {
            if (holder.value() instanceof ShapedRecipe shaped) {
                final ItemStack result = shaped.assemble(CraftingInput.EMPTY);
                if (BuiltInRegistries.ITEM.getKey(result.getItem()).equals(polymer)) {
                    counts.add(result.getCount());
                }
            }
        }
        helper.assertTrue(counts.equals(Set.of(2, 4)),
                Component.literal("polymer recipes give " + counts + ", expected 4 from slime and 2 from sugar"));
        helper.succeed();
    }

    private static void tiersAreCraftedInOrder(final GameTestHelper helper) {
        final List<String> devices = new ArrayList<>(DEVICES);
        devices.add("energy_cell");
        for (String device : devices) {
            for (int tier = 1; tier < TIERS.size(); tier++) {
                final ItemStack lower = stackOf(TIERS.get(tier - 1) + "_" + device);
                final ItemStack upgrade = stackOf(TIERS.get(tier) + "_tier_upgrade");
                final Item expected = stackOf(TIERS.get(tier) + "_" + device).getItem();
                helper.assertTrue(craftedFrom(helper, lower, upgrade) == expected,
                        Component.literal(TIERS.get(tier) + " " + device + " is not crafted from the tier below and "
                                + "its upgrade"));
                if (tier + 1 < TIERS.size()) {
                    final ItemStack skipped = stackOf(TIERS.get(tier + 1) + "_tier_upgrade");
                    helper.assertTrue(craftedFrom(helper, lower, skipped) == null,
                            Component.literal(TIERS.get(tier) + " " + device + " skipped a tier with an upgrade"));
                }
            }
        }
        helper.succeed();
    }

    private static void earlyStorageAndScreen(final GameTestHelper helper) {
        final ItemStack glass = new ItemStack(Items.GLASS);
        final ItemStack plate = stackOf("steel_plate");
        final ItemStack redstone = new ItemStack(Items.REDSTONE);
        final Item screen = craftedGrid(helper, List.of(glass, glass, glass, plate, redstone, plate,
                ItemStack.EMPTY, plate, ItemStack.EMPTY));
        helper.assertTrue(screen == stackOf("screen").getItem(),
                Component.literal("glass, steel plates and redstone do not make a Screen: " + screen));
        final ItemStack steel = stackOf("steel_ingot");
        final ItemStack chest = new ItemStack(Items.CHEST);
        final ItemStack hopper = new ItemStack(Items.HOPPER);
        final Item vault = craftedGrid(helper, List.of(steel, chest, steel, hopper, stackOf("core"), hopper, steel,
                redstone, steel));
        helper.assertTrue(vault == stackOf("external_vault").getItem(),
                Component.literal("steel, a chest, hoppers, a Core and redstone do not make an External Vault: "
                        + vault));
        helper.succeed();
    }

    private static @Nullable Item craftedGrid(final GameTestHelper helper, final List<ItemStack> nine) {
        final CraftingInput input = CraftingInput.of(3, 3, nine);
        return helper.getLevel().getServer().getRecipeManager().recipeMap()
                .getRecipesFor(RecipeType.CRAFTING, input, helper.getLevel())
                .findFirst()
                .map(holder -> holder.value().assemble(input).getItem())
                .orElse(null);
    }

    private static ItemStack stackOf(final String name) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name)));
    }

    private static @Nullable Item craftedFrom(final GameTestHelper helper, final ItemStack first,
                                              final ItemStack second) {
        final CraftingInput input = CraftingInput.of(2, 1, List.of(first, second));
        return helper.getLevel().getServer().getRecipeManager().recipeMap()
                .getRecipesFor(RecipeType.CRAFTING, input, helper.getLevel())
                .findFirst()
                .map(holder -> holder.value().assemble(input).getItem())
                .orElse(null);
    }

    private static void assertCrafted(final GameTestHelper helper, final List<String> names) {
        final Set<Identifier> results = new HashSet<>();
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().recipeMap().values()) {
            if (holder.value() instanceof ShapedRecipe shaped) {
                results.add(BuiltInRegistries.ITEM.getKey(shaped.assemble(CraftingInput.EMPTY).getItem()));
            } else if (holder.value() instanceof ShapelessRecipe shapeless) {
                results.add(BuiltInRegistries.ITEM.getKey(shapeless.assemble(CraftingInput.EMPTY).getItem()));
            }
        }
        for (String name : names) {
            final Identifier item = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            helper.assertTrue(results.contains(item), Component.literal("no crafting recipe makes " + item));
        }
    }
}
