package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.item.StoredFluids;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.processing.MachineFacing;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusFluids;
import com.morphengine.nexus.registry.NexusMaterials;
import com.morphengine.nexus.registry.NexusRecipes;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The generators in a world: what each burns, what its slot and a bucket in hand do with a container of fluid, and how
 * the block shows that it works.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class GeneratorGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 300;
    private static final BlockPos GENERATOR = new BlockPos(2, 1, 2);
    private static final int BUCKET = 1000;
    private static final int STAR_BURN_TICKS = 24_000;
    /** The plants the Crusher grinds into biomass, one recipe each. */
    private static final int PLANT_RECIPES = 32;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("coal_generator_burns_coal", GeneratorGameTests::coalBurns),
            Map.entry("nether_star_generator_burns_a_star_for_twenty_minutes", GeneratorGameTests::starBurns),
            Map.entry("lava_generator_takes_a_bucket_from_its_slot_and_leaves_it_empty", GeneratorGameTests::lavaSlot),
            Map.entry("lava_generator_burns_the_lava_of_its_tank", GeneratorGameTests::lavaBurns),
            Map.entry("biofuel_generator_burns_biofuel_the_best", GeneratorGameTests::biofuelBurns),
            Map.entry("steam_generator_needs_both_water_and_lava", GeneratorGameTests::steamNeedsBoth),
            Map.entry("a_tank_takes_only_its_own_fluid", GeneratorGameTests::tankTakesOnlyItsFluid),
            Map.entry("a_bucket_in_hand_pours_into_the_tank", GeneratorGameTests::bucketInHandPours),
            Map.entry("a_bucket_in_hand_does_not_pour_the_wrong_fluid", GeneratorGameTests::wrongBucketStays),
            Map.entry("a_generator_takes_only_what_it_burns_in_its_slot", GeneratorGameTests::slotFilter),
            Map.entry("the_block_shows_whether_it_works", GeneratorGameTests::phaseFollowsWork),
            Map.entry("the_extractor_presses_biofuel_out_of_plants", GeneratorGameTests::extractorPressesBiofuel),
            Map.entry("an_empty_bucket_in_hand_fills_from_the_extractor", GeneratorGameTests::bucketFillsFromExtractor),
            Map.entry("a_full_bucket_does_not_pour_into_the_extractor", GeneratorGameTests::extractorTakesNothing),
            Map.entry("an_entity_moves_in_biofuel_and_does_not_drown", GeneratorGameTests::movesInBiofuel),
            Map.entry("a_side_of_a_generator_takes_and_gives_as_it_is_set", GeneratorGameTests::sidesLimitHandlers),
            Map.entry("a_side_that_takes_in_lets_coal_in_but_never_out", GeneratorGameTests::sideTakesCoalIn));

    private GeneratorGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "generators"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static GeneratorBlockEntity place(final GameTestHelper helper, final GeneratorKind kind) {
        helper.setBlock(GENERATOR, NexusBlocks.GENERATORS.get(kind).get().defaultBlockState());
        return helper.getBlockEntity(GENERATOR, GeneratorBlockEntity.class);
    }

    private static void assertValue(final GameTestHelper helper, final long actual, final long expected,
                                    final String what) {
        helper.assertValueEqual(actual, expected, Component.literal(what));
    }

    private static void coalBurns(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.COAL);
        generator.input().setItem(0, new ItemStack(Items.COAL));

        helper.startSequence()
                .thenWaitUntil(() -> assertValue(helper, generator.view().production(),
                        GeneratorKind.COAL.outputPerTick(), "FE a tick"))
                .thenSucceed();
    }

    private static void starBurns(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.NETHER_STAR);
        generator.input().setItem(0, new ItemStack(Items.NETHER_STAR));

        helper.startSequence()
                .thenWaitUntil(() -> assertValue(helper, generator.view().burnTicksTotal(), STAR_BURN_TICKS,
                        "ticks a nether star burns"))
                .thenExecute(() -> assertValue(helper, generator.view().production(),
                        GeneratorKind.NETHER_STAR.outputPerTick(), "FE a tick"))
                .thenSucceed();
    }

    private static void lavaSlot(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.LAVA);
        generator.input().setItem(0, new ItemStack(Items.LAVA_BUCKET));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(generator.input().getItem(0).is(Items.BUCKET),
                        Component.literal("the bucket in the slot is not empty yet")))
                .thenExecute(() -> helper.assertTrue(generator.view().tanks().getFirst().amount() > 0,
                        Component.literal("the lava is not in the tank")))
                .thenSucceed();
    }

    private static void lavaBurns(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.LAVA);
        generator.input().setItem(0, new ItemStack(Items.LAVA_BUCKET));

        helper.startSequence()
                .thenWaitUntil(() -> assertValue(helper, generator.view().production(),
                        GeneratorKind.LAVA.outputPerTick(), "FE a tick"))
                .thenSucceed();
    }

    private static void biofuelBurns(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.BIOFUEL);
        generator.input().setItem(0, new ItemStack(NexusFluids.BIOFUEL_BUCKET.get()));

        helper.startSequence()
                .thenWaitUntil(() -> assertValue(helper, generator.view().production(),
                        GeneratorKind.BIOFUEL.outputPerTick(), "FE a tick"))
                .thenExecute(() -> helper.assertTrue(
                        GeneratorKind.BIOFUEL.energyPerTick() > GeneratorKind.STEAM.energyPerTick()
                                && GeneratorKind.STEAM.energyPerTick() > GeneratorKind.LAVA.energyPerTick(),
                        Component.literal("biofuel is not the best, steam the next")))
                .thenSucceed();
    }

    private static void steamNeedsBoth(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.STEAM);
        insert(generator, FluidResource.of(Fluids.LAVA), BUCKET);

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> assertValue(helper, generator.view().production(), 0, "FE a tick without water"))
                .thenExecute(() -> insert(generator, FluidResource.of(Fluids.WATER), BUCKET))
                .thenWaitUntil(() -> assertValue(helper, generator.view().production(),
                        GeneratorKind.STEAM.outputPerTick(), "FE a tick with both"))
                .thenSucceed();
    }

    private static int insert(final GeneratorBlockEntity generator, final FluidResource fluid, final int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int inserted = generator.fluidHandler(null).insert(fluid, amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    private static void tankTakesOnlyItsFluid(final GameTestHelper helper) {
        final GeneratorBlockEntity lava = place(helper, GeneratorKind.LAVA);
        assertValue(helper, insert(lava, FluidResource.of(Fluids.WATER), BUCKET), 0, "water into the lava tank");
        assertValue(helper, insert(lava, FluidResource.of(Fluids.LAVA), BUCKET), BUCKET, "lava into the lava tank");
        helper.assertTrue(place(helper, GeneratorKind.COAL).fluidHandler(null) == null,
                Component.literal("a coal generator has a tank"));
        helper.succeed();
    }

    private static ServerPlayer holding(final GameTestHelper helper, final ItemStack stack) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static InteractionResult useOn(final GameTestHelper helper, final ServerPlayer player) {
        final BlockPos absolute = helper.absolutePos(GENERATOR);
        final ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        return helper.getLevel().getBlockState(absolute).useItemOn(stack, helper.getLevel(), player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
    }

    private static void bucketInHandPours(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.LAVA);
        final ServerPlayer player = holding(helper, new ItemStack(Items.LAVA_BUCKET));

        final InteractionResult result = useOn(helper, player);

        helper.assertTrue(result.consumesAction(), Component.literal("the bucket was not used"));
        assertValue(helper, generator.view().tanks().getFirst().amount(), BUCKET, "lava in the tank");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                Component.literal("the bucket in hand is not empty"));
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static void wrongBucketStays(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.LAVA);
        final ServerPlayer player = holding(helper, new ItemStack(Items.WATER_BUCKET));

        useOn(helper, player);

        assertValue(helper, generator.view().tanks().getFirst().amount(), 0, "water in the lava tank");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET),
                Component.literal("the water bucket was emptied"));
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static void slotFilter(final GameTestHelper helper) {
        final GeneratorBlockEntity coal = place(helper, GeneratorKind.COAL);
        helper.assertTrue(coal.kind().takesIn(new ItemStack(Items.COAL))
                && !coal.kind().takesIn(new ItemStack(Items.DIRT))
                && !coal.kind().takesIn(new ItemStack(Items.LAVA_BUCKET)), Component.literal("the coal slot"));
        final GeneratorBlockEntity star = place(helper, GeneratorKind.NETHER_STAR);
        helper.assertTrue(star.kind().takesIn(new ItemStack(Items.NETHER_STAR))
                && !star.kind().takesIn(new ItemStack(Items.COAL)), Component.literal("the star slot"));
        final GeneratorBlockEntity lava = place(helper, GeneratorKind.LAVA);
        helper.assertTrue(lava.kind().takesIn(new ItemStack(Items.BUCKET))
                && lava.kind().takesIn(new ItemStack(Items.WATER_BUCKET))
                && !lava.kind().takesIn(new ItemStack(Items.COAL)), Component.literal("the bucket slot"));
        helper.succeed();
    }

    private static void phaseFollowsWork(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.COAL);
        helper.assertValueEqual(helper.getBlockState(GENERATOR).getValue(GeneratorBlock.PHASE), MachinePhase.OFF,
                Component.literal("a new generator"));
        generator.input().setItem(0, new ItemStack(Items.COAL));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(
                        helper.getBlockState(GENERATOR).getValue(GeneratorBlock.PHASE), MachinePhase.ACTIVE,
                        Component.literal("a generator at work")))
                .thenSucceed();
    }

    private static MachineBlockEntity placeExtractor(final GameTestHelper helper, final int millibuckets) {
        helper.setBlock(GENERATOR,
                NexusBlocks.machineTiers(MachineKind.EXTRACTOR).getFirst().get().defaultBlockState());
        final MachineBlockEntity extractor = helper.getBlockEntity(GENERATOR, MachineBlockEntity.class);
        final ItemStack seed = new ItemStack(Items.STONE);
        seed.set(NexusDataComponents.STORED_FLUIDS.get(),
                new StoredFluids(List.of(new FluidStack(Fluids.WATER, millibuckets))));
        extractor.applyComponentsFromItemStack(seed);
        return extractor;
    }

    private static void bucketFillsFromExtractor(final GameTestHelper helper) {
        final MachineBlockEntity extractor = placeExtractor(helper, 2 * BUCKET);
        final ServerPlayer player = holding(helper, new ItemStack(Items.BUCKET));

        final InteractionResult result = useOn(helper, player);

        helper.assertTrue(result.consumesAction(), Component.literal("the bucket was not used"));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET),
                Component.literal("the bucket in hand is not full"));
        assertValue(helper, extractor.view().tank().amount(), BUCKET, "water left in the tank");
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static void extractorTakesNothing(final GameTestHelper helper) {
        final MachineBlockEntity extractor = placeExtractor(helper, 0);
        final ServerPlayer player = holding(helper, new ItemStack(Items.WATER_BUCKET));

        useOn(helper, player);

        assertValue(helper, extractor.view().tank().amount(), 0, "water poured into the extractor");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET),
                Component.literal("the water bucket was emptied"));
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    private static void extractorPressesBiofuel(final GameTestHelper helper) {
        final var manager = helper.getLevel().getServer().getRecipeManager().recipeMap();
        final var extracting = manager.byType(NexusRecipes.EXTRACTING.get()).stream().map(holder -> holder.value())
                .filter(recipe -> recipe.fluid().isSame(NexusFluids.BIOFUEL.get())).toList();
        assertValue(helper, extracting.size(), 1, "recipes of biofuel");
        helper.assertTrue(extracting.getFirst().ingredient().test(new ItemStack(NexusMaterials.BIOMASS.get())),
                Component.literal("biofuel is not pressed out of biomass"));
        final var crushing = manager.byType(NexusRecipes.CRUSHING.get()).stream().map(holder -> holder.value())
                .filter(recipe -> recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY))
                        .is(NexusMaterials.BIOMASS.get()))
                .toList();
        assertValue(helper, crushing.size(), PLANT_RECIPES, "recipes of biomass");
        for (var plant : List.of(Items.OAK_SAPLING, Items.OAK_LEAVES, Items.OAK_LOG, Items.WHEAT, Items.DANDELION,
                Items.SHORT_GRASS, Items.WHEAT_SEEDS)) {
            helper.assertTrue(crushing.stream().anyMatch(recipe -> recipe.input().test(new ItemStack(plant))),
                    Component.literal("no biomass out of " + plant));
        }
        helper.assertTrue(crushing.stream().noneMatch(recipe -> recipe.input().test(new ItemStack(Items.STONE))),
                Component.literal("biomass out of stone"));
        helper.succeed();
    }

    private static void movesInBiofuel(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, NexusFluids.BIOFUEL_BLOCK.get().defaultBlockState());
        final Zombie zombie = helper.spawn(EntityType.ZOMBIE, pos);
        final FluidType biofuel = NexusFluids.BIOFUEL_TYPE.get();
        final Vec3 before = zombie.position();

        final boolean handled = biofuel.move(zombie, new Vec3(0, 0, 1), 0.08);

        helper.assertTrue(handled, Component.literal("the entity is not moved by the fluid"));
        helper.assertTrue(zombie.position().distanceToSqr(before) > 0, Component.literal("the entity did not move"));
        helper.assertTrue(!biofuel.canDrownIn(zombie), Component.literal("the entity drowns in biofuel"));
        helper.succeed();
    }

    private static Direction worldSide(final MachineSide side) {
        return MachineFacing.worldSide(Direction.NORTH, side);
    }

    private static void sidesLimitHandlers(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.LAVA);
        generator.setSideMode(MachineSide.TOP, SideMode.CLOSED);
        generator.setSideMode(MachineSide.BOTTOM, SideMode.OUTPUT);
        generator.setSideMode(MachineSide.LEFT, SideMode.INPUT);

        final Direction closed = worldSide(MachineSide.TOP);
        final Direction gives = worldSide(MachineSide.BOTTOM);
        final Direction takes = worldSide(MachineSide.LEFT);
        helper.assertTrue(generator.energyHandler(closed) == null && generator.fluidHandler(closed) == null
                && generator.itemHandler(closed) == null, Component.literal("a closed side shows something"));
        helper.assertTrue(generator.energyHandler(gives) != null && generator.fluidHandler(gives) == null,
                Component.literal("an output side takes fuel in or gives no FE"));
        helper.assertTrue(generator.energyHandler(takes) == null && generator.fluidHandler(takes) != null
                && generator.itemHandler(takes) != null, Component.literal("an input side gives FE or takes no fuel"));
        helper.assertTrue(generator.energyHandler(worldSide(MachineSide.FRONT)) != null,
                Component.literal("a side that was left open stopped giving FE"));
        helper.succeed();
    }

    private static void sideTakesCoalIn(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, GeneratorKind.COAL);
        final var items = generator.itemHandler(worldSide(MachineSide.BACK));
        final ItemResource coal = ItemResource.of(Items.COAL);

        try (Transaction transaction = Transaction.openRoot()) {
            assertValue(helper, items.insert(coal, 5, transaction), 5, "coal that goes in");
            transaction.commit();
        }
        helper.assertTrue(generator.input().getItem(0).is(Items.COAL),
                Component.literal("the coal is not in the slot"));
        try (Transaction transaction = Transaction.openRoot()) {
            assertValue(helper, items.extract(coal, 5, transaction), 0, "coal that comes out");
        }
        try (Transaction transaction = Transaction.openRoot()) {
            assertValue(helper, items.insert(ItemResource.of(Items.DIRT), 1, transaction), 0, "dirt that goes in");
        }
        helper.succeed();
    }
}
