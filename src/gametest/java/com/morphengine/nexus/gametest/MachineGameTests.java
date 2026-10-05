package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.machine.InputMode;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.machine.MachineTier;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.resource.ItemKey;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * The machines in the world: they take what has a recipe through the sides that let things in, work on FE, give the
 * result through the sides that let things out, share a stack over their lines when told to, keep their contents when
 * upgraded or saved, and draw their energy from their network.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class MachineGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 400;
    private static final BlockPos MACHINE = new BlockPos(3, 1, 3);
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos ON_NETWORK = NEXUS.east();
    private static final int BUFFER_TOP_UPS = 10;
    private static final int PER_TOP_UP = 1_000;
    private static final int CELL_CHARGE = 1_000;
    private static final int STACK = 9;
    private static final int LINES_OF_ADVANCED = 3;
    private static final int ICE_MILLIBUCKETS = 1_000;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("machine_smelts_what_it_is_given_and_gives_the_result", MachineGameTests::smelts),
            Map.entry("machine_takes_only_what_has_a_recipe", MachineGameTests::takesOnlyRecipes),
            Map.entry("machine_sides_follow_the_front_and_closed_sides_show_nothing", MachineGameTests::sides),
            Map.entry("machine_lets_things_in_at_the_top_and_out_at_the_bottom", MachineGameTests::topInBottomOut),
            Map.entry("machine_in_split_mode_spreads_a_stack_over_its_lines", MachineGameTests::splits),
            Map.entry("machine_upgraded_keeps_its_contents_and_gets_more_lines", MachineGameTests::upgrades),
            Map.entry("machine_keeps_everything_through_saving", MachineGameTests::keepsThroughSaving),
            Map.entry("machine_draws_energy_from_its_network_and_shows_it_works", MachineGameTests::drawsFromNetwork),
            Map.entry("crusher_turns_cobblestone_into_two_gravel", helper -> makes(helper, MachineKind.CRUSHER,
                    Items.COBBLESTONE, Items.GRAVEL, 2)),
            Map.entry("pulverizer_turns_gravel_into_sand", helper -> makes(helper, MachineKind.PULVERIZER,
                    Items.GRAVEL, Items.SAND, 1)),
            Map.entry("alloy_smelter_melts_two_inputs_from_separate_slots_into_one_result", MachineGameTests::alloys),
            Map.entry("assembler_reaches_a_machine_whose_sides_are_all_closed",
                    MachineGameTests::assemblerThroughClosedSides),
            Map.entry("alloy_smelter_takes_the_ticks_its_recipe_says", MachineGameTests::alloyDuration),
            Map.entry("alloy_smelter_tells_clients_its_cycle_is_running", MachineGameTests::tellsCycle),
            Map.entry("alloy_smelter_tells_clients_what_it_holds_and_what_it_makes", MachineGameTests::showsItems),
            Map.entry("extractor_presses_water_out_of_ice_into_its_tank", MachineGameTests::extracts),
            Map.entry("compressor_turns_sand_into_sandstone", helper -> makes(helper, MachineKind.COMPRESSOR,
                    Items.SAND, Items.SANDSTONE, 1)));

    private MachineGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "machines"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void smelts(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);
        fill(machine);
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.RAW_IRON, 2), 2,
                Component.literal("raw iron put in at the top"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.slots().outputs().getItem(0).is(Items.IRON_INGOT),
                        Component.literal("no iron ingot yet")))
                .thenExecute(() -> helper.assertValueEqual(machine.slots().inputs().getItem(0).getCount(), 1,
                        Component.literal("raw iron left after one job")))
                .thenSucceed();
    }

    private static void takesOnlyRecipes(final GameTestHelper helper) {
        furnace(helper, MACHINE, 0);

        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);

        helper.assertValueEqual(insert(top, Items.DIRT, 1), 0, Component.literal("dirt, which nothing cooks"));
        helper.assertValueEqual(insert(top, Items.PORKCHOP, 1), 1, Component.literal("raw pork, which cooks"));
        helper.succeed();
    }

    private static void sides(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);

        helper.assertTrue(handler(helper, MACHINE, Direction.EAST) != null,
                Component.literal("the left side of a machine that faces north, where things go in"));
        machine.setSideMode(MachineSide.LEFT, SideMode.CLOSED);
        helper.assertTrue(handler(helper, MACHINE, Direction.EAST) == null,
                Component.literal("the left side after it was closed"));
        helper.assertTrue(handler(helper, MACHINE, Direction.WEST) != null,
                Component.literal("the right side, which lets things out"));
        machine.setSideMode(MachineSide.FRONT, SideMode.BOTH);
        helper.assertTrue(handler(helper, MACHINE, Direction.NORTH) != null,
                Component.literal("the front after it was opened"));
        helper.succeed();
    }

    private static void topInBottomOut(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);
        machine.slots().outputs().setItem(0, new ItemStack(Items.IRON_INGOT, 3));

        helper.assertValueEqual(extract(handler(helper, MACHINE, Direction.DOWN), Items.IRON_INGOT, 2), 2,
                Component.literal("ingots taken out at the bottom"));
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.DOWN), Items.RAW_IRON, 1), 0,
                Component.literal("raw iron put in at the bottom, which lets things out only"));
        helper.assertValueEqual(extract(handler(helper, MACHINE, Direction.UP), Items.IRON_INGOT, 1), 0,
                Component.literal("an ingot taken out at the top, which lets things in only"));
        helper.succeed();
    }

    private static void splits(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 1);
        machine.machine().inventory().setMode(InputMode.SPLIT);

        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.RAW_IRON, STACK), STACK,
                Component.literal("raw iron put in as a stack"));
        for (int line = 0; line < LINES_OF_ADVANCED; line++) {
            helper.assertValueEqual(machine.slots().inputs().getItem(line).getCount(), STACK / LINES_OF_ADVANCED,
                    Component.literal("raw iron in input slot " + line));
        }
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.RAW_GOLD, 1), 0,
                Component.literal("another resource while one is spread over the slots"));
        helper.succeed();
    }

    private static void upgrades(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);
        machine.machine().energy().insert(PER_TOP_UP, Action.EXECUTE);
        machine.slots().inputs().setItem(0, new ItemStack(Items.RAW_IRON, 5));
        machine.setSideMode(MachineSide.BACK, SideMode.CLOSED);

        final BlockState before = helper.getBlockState(MACHINE);
        helper.setBlock(MACHINE, NexusBlocks.machineTiers(MachineKind.ENERGY_FURNACE).get(1).get()
                .withPropertiesOf(before));

        final MachineBlockEntity upgraded = helper.getBlockEntity(MACHINE, MachineBlockEntity.class);
        helper.assertTrue(upgraded == machine, Component.literal("the block entity was replaced"));
        helper.assertValueEqual(upgraded.machine().lineCount(), LINES_OF_ADVANCED, Component.literal("lines"));
        helper.assertValueEqual(upgraded.machine().energy().stored(), (long) PER_TOP_UP,
                Component.literal("FE kept"));
        helper.assertValueEqual(upgraded.slots().inputs().getItem(0).getCount(), 5,
                Component.literal("raw iron kept"));
        helper.assertValueEqual(upgraded.machine().sides().mode(MachineSide.BACK), SideMode.CLOSED,
                Component.literal("a setting kept"));
        helper.assertValueEqual(upgraded.machine().energy().capacity(), MachineTier.ADVANCED.bufferCapacity(),
                Component.literal("buffer of the new tier"));
        helper.succeed();
    }

    private static void keepsThroughSaving(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 1);
        machine.machine().energy().insert(PER_TOP_UP, Action.EXECUTE);
        machine.slots().inputs().setItem(1, new ItemStack(Items.RAW_COPPER, 7));
        machine.slots().outputs().setItem(2, new ItemStack(Items.IRON_INGOT, 4));
        machine.machine().inventory().setMode(InputMode.SPLIT);
        machine.setSideMode(MachineSide.TOP, SideMode.OUTPUT);

        final MachineBlockEntity loaded = new MachineBlockEntity(helper.absolutePos(MACHINE),
                helper.getBlockState(MACHINE));
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(),
                machine.saveCustomOnly(helper.getLevel().registryAccess())));

        helper.assertValueEqual(loaded.machine().energy().stored(), (long) PER_TOP_UP, Component.literal("FE"));
        helper.assertValueEqual(loaded.slots().inputs().getItem(1).getCount(), 7, Component.literal("input"));
        helper.assertValueEqual(loaded.slots().outputs().getItem(2).getCount(), 4, Component.literal("output"));
        helper.assertValueEqual(loaded.machine().inventory().mode(), InputMode.SPLIT, Component.literal("mode"));
        helper.assertValueEqual(loaded.machine().sides().mode(MachineSide.TOP), SideMode.OUTPUT,
                Component.literal("a side"));
        helper.succeed();
    }

    private static void drawsFromNetwork(final GameTestHelper helper) {
        placeBlock(helper, NEXUS, NexusBlocks.NEXUS.get());
        placeBlock(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get());
        TestEnergy.charge(helper, CELL, CELL_CHARGE);
        final MachineBlockEntity machine = furnace(helper, ON_NETWORK, 0);
        machine.slots().inputs().setItem(0, new ItemStack(Items.RAW_IRON, 1));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.machine().line(0).isRunning()
                        || !machine.slots().outputs().getItem(0).isEmpty(),
                        Component.literal("the machine has not started on the energy of its network")))
                .thenExecute(() -> helper.assertTrue(helper.getBlockState(ON_NETWORK).getValue(MachineBlock.PHASE)
                        != MachinePhase.OFF, Component.literal("the machine shows no energy")))
                .thenSucceed();
    }

    private static void makes(
            final GameTestHelper helper, final MachineKind kind, final Item input, final Item result,
            final int count) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(kind).getFirst().get());
        fill(machine);
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), input, 1), 1,
                Component.literal(input + " put in"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.slots().outputs().getItem(0).is(result),
                        Component.literal("no " + result + " yet")))
                .thenExecute(() -> helper.assertValueEqual(machine.slots().outputs().getItem(0).getCount(), count,
                        Component.literal("count of " + result)))
                .thenSucceed();
    }

    private static void alloys(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.ALLOY_SMELTER).getFirst().get());
        fill(machine);
        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);
        helper.assertValueEqual(insert(top, Items.IRON_INGOT, 1), 1, Component.literal("iron ingot put in"));
        helper.assertValueEqual(insert(top, Items.COAL, 1), 1, Component.literal("coal put in"));
        helper.assertValueEqual(insert(top, Items.DIRT, 1), 0, Component.literal("dirt, which no alloy uses"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        machine.slots().outputs().getItem(0).is(Items.NETHERITE_SCRAP),
                        Component.literal("no result yet")))
                .thenSucceed();
    }

    private static void assemblerThroughClosedSides(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);
        fill(machine);
        for (MachineSide side : MachineSide.values()) {
            machine.setSideMode(side, SideMode.CLOSED);
        }
        final SideStorage access = machine.assemblerAccess();
        final ItemKey raw = ItemKey.of(new ItemStack(Items.RAW_IRON));
        final ItemKey ingot = ItemKey.of(new ItemStack(Items.IRON_INGOT));
        helper.assertValueEqual(access.insert(raw, 2, Action.EXECUTE, Actor.NOBODY), 2L,
                Component.literal("raw iron put in through closed sides"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        access.extract(ingot, 1, Action.SIMULATE, Actor.NOBODY) == 1L,
                        Component.literal("no ingot to take through closed sides yet")))
                .thenSucceed();
    }

    private static void alloyDuration(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.ALLOY_SMELTER).getFirst().get());
        fill(machine);
        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);
        insert(top, Items.IRON_INGOT, 1);
        insert(top, Items.COAL, 1);
        final long started = helper.getLevel().getGameTime();
        final long expected = Math.ceilDiv(160L * 100, machine.machine().speedPercent());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        machine.slots().outputs().getItem(0).is(Items.NETHERITE_SCRAP),
                        Component.literal("no result yet")))
                .thenExecute(() -> helper.assertTrue(
                        Math.abs(helper.getLevel().getGameTime() - started - expected) <= 3,
                        Component.literal("ticks taken " + (helper.getLevel().getGameTime() - started)
                                + ", expected " + expected)))
                .thenSucceed();
    }

    private static void tellsCycle(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.ALLOY_SMELTER).getFirst().get());
        fill(machine);
        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);
        insert(top, Items.IRON_INGOT, 1);
        insert(top, Items.COAL, 1);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.getUpdateTag(helper.getLevel().registryAccess())
                        .getBooleanOr("cycle_active", false), Component.literal("cycle not sent")))
                .thenSucceed();
    }

    private static void showsItems(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.ALLOY_SMELTER).getFirst().get());
        fill(machine);
        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);
        insert(top, Items.IRON_INGOT, 1);
        insert(top, Items.COAL, 1);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.shownItem(0).is(Items.IRON_INGOT)
                        && machine.shownItem(1).is(Items.COAL), Component.literal("inputs not shown")))
                .thenWaitUntil(() -> helper.assertTrue(machine.shownItem(3).is(Items.NETHERITE_SCRAP),
                        Component.literal("result not shown while it works")))
                .thenSucceed();
    }

    private static void extracts(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.EXTRACTOR).getFirst().get());
        fill(machine);
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.ICE, 1), 1,
                Component.literal("ice put in"));
        final ResourceHandler<FluidResource> tank = helper.getLevel().getCapability(
                Capabilities.Fluid.BLOCK, helper.absolutePos(MACHINE), Direction.DOWN);
        helper.assertTrue(tank != null, Component.literal("the bottom of an Extractor gives no fluid"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(tank.getAmountAsInt(0), ICE_MILLIBUCKETS,
                        Component.literal("millibuckets in the tank")))
                .thenExecute(() -> helper.assertTrue(tank.getResource(0).getFluid() == Fluids.WATER,
                        Component.literal("the fluid in the tank")))
                .thenExecute(() -> helper.assertValueEqual(putIntoTank(tank), 0,
                        Component.literal("water put into the tank from outside")))
                .thenSucceed();
    }

    private static int putIntoTank(final ResourceHandler<FluidResource> tank) {
        try (Transaction transaction = Transaction.openRoot()) {
            return tank.insert(FluidResource.of(Fluids.WATER), 1, transaction);
        }
    }

    private static MachineBlockEntity furnace(final GameTestHelper helper, final BlockPos pos, final int tier) {
        return placeMachine(helper, pos, NexusBlocks.machineTiers(MachineKind.ENERGY_FURNACE).get(tier).get());
    }

    private static void placeBlock(final GameTestHelper helper, final BlockPos pos, final Block block) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(block.defaultBlockState(), helper.getLevel(),
                helper.absolutePos(pos)));
    }

    private static MachineBlockEntity placeMachine(final GameTestHelper helper, final BlockPos pos, final Block block) {
        placeBlock(helper, pos, block);
        return helper.getBlockEntity(pos, MachineBlockEntity.class);
    }

    private static void fill(final MachineBlockEntity machine) {
        for (int top = 0; top < BUFFER_TOP_UPS; top++) {
            machine.machine().energy().insert(PER_TOP_UP, Action.EXECUTE);
        }
    }

    private static @Nullable ResourceHandler<ItemResource> handler(
            final GameTestHelper helper, final BlockPos pos, final Direction side) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), side);
    }

    private static int insert(
            final @Nullable ResourceHandler<ItemResource> handler, final Item item, final int amount) {
        if (handler == null) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int accepted = handler.insert(ItemResource.of(item), amount, transaction);
            transaction.commit();
            return accepted;
        }
    }

    private static int extract(
            final @Nullable ResourceHandler<ItemResource> handler, final Item item, final int amount) {
        if (handler == null) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            final int taken = handler.extract(ItemResource.of(item), amount, transaction);
            transaction.commit();
            return taken;
        }
    }
}
