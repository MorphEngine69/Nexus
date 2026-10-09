package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.machine.InputMode;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.machine.MachineTier;
import com.morphengine.nexus.metal.VanillaMetal;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusMaterials;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.transfer.FluidResource;
import com.morphengine.nexus.transfer.ItemResource;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The machines in the world: they take what has a recipe through the sides that let things in, work on FE, give the
 * result through the sides that let things out, share a stack over their lines when told to, keep their contents when
 * upgraded or saved, and draw their energy from their network.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class MachineGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 400;
    private static final BlockPos MACHINE = new BlockPos(3, 1, 3);
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos ON_NETWORK = NEXUS.east();
    private static final int BUFFER_TOP_UPS = 10;
    private static final int DRAW_TEST_TICKS = 20;
    private static final int DRAW_TEST_CHARGE = 20_000;
    private static final int PER_TOP_UP = (int) MachineTier.BASIC.maxInsert();
    private static final int CELL_CHARGE = 1_000;
    private static final int STACK = 9;
    private static final int LINES_OF_ADVANCED = 3;
    private static final int ICE_MILLIBUCKETS = 1_000;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("machine_smelts_what_it_is_given_and_gives_the_result", MachineGameTests::smelts),
            Map.entry("a_machine_draws_from_one_cell_no_more_than_the_cell_gives_in_one_go",
                    MachineGameTests::oneCellLimitsTheDraw),
            Map.entry("two_cells_give_a_machine_twice_as_much_in_a_tick", MachineGameTests::twoCellsDoubleTheDraw),
            Map.entry("first_compressor_runs_on_generators_beside_it_without_a_network",
                    MachineGameTests::compressorRunsOnGeneratorsAlone),
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
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(MachineGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void smelts(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);
        fill(machine);
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.RAW_IRON, 2), 2,
                String.valueOf("raw iron put in at the top"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.slots().outputs().getItem(0).is(Items.IRON_INGOT),
                        String.valueOf("no iron ingot yet")))
                .thenExecute(() -> helper.assertValueEqual(machine.slots().inputs().getItem(0).getCount(), 1,
                        String.valueOf("raw iron left after one job")))
                .thenSucceed();
    }

    private static void oneCellLimitsTheDraw(final GameTestHelper helper) {
        placeBlock(helper, NEXUS, NexusBlocks.NEXUS.get());
        placeBlock(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get());
        TestEnergy.fill(helper, CELL, DRAW_TEST_CHARGE);
        final MachineBlockEntity machine = placeMachine(helper, ON_NETWORK,
                NexusBlocks.machineTiers(MachineKind.COMPRESSOR).get(2).get());

        helper.startSequence()
                .thenIdle(DRAW_TEST_TICKS)
                .thenExecute(() -> assertDrawn(helper, machine, 1))
                .thenSucceed();
    }

    private static void twoCellsDoubleTheDraw(final GameTestHelper helper) {
        placeBlock(helper, NEXUS, NexusBlocks.NEXUS.get());
        placeBlock(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get());
        placeBlock(helper, NEXUS.north(), NexusBlocks.BASIC_ENERGY_CELL.get());
        TestEnergy.fill(helper, CELL, DRAW_TEST_CHARGE);
        TestEnergy.fill(helper, NEXUS.north(), DRAW_TEST_CHARGE);
        final MachineBlockEntity machine = placeMachine(helper, ON_NETWORK,
                NexusBlocks.machineTiers(MachineKind.COMPRESSOR).get(2).get());

        helper.startSequence()
                .thenIdle(DRAW_TEST_TICKS)
                .thenExecute(() -> assertDrawn(helper, machine, 2))
                .thenSucceed();
    }

    /**
     * A machine fills its buffer from the pool once a tick, and a Basic cell gives at most its transfer limit in one
     * go, so the buffer holds about that much for every tick and every cell, never more.
     */
    private static void assertDrawn(final GameTestHelper helper, final MachineBlockEntity machine, final int cells) {
        final long limit = cells * EnergyCellTier.BASIC.maxTransfer() * DRAW_TEST_TICKS;
        final long drawn = machine.machine().energy().stored();
        helper.assertTrue(drawn <= limit && drawn >= limit * 8 / 10,
                String.valueOf("the machine drew " + drawn + " FE in " + DRAW_TEST_TICKS + " ticks from " + cells
                        + " Basic cells, expected up to " + limit));
    }

    /**
     * Where a player starts: a Coal Generator that needs no Battery, and a Compressor, which makes the plates the
     * Battery and the rest need. Four generators stand round it, since one makes less FE than the Compressor takes.
     */
    private static void compressorRunsOnGeneratorsAlone(final GameTestHelper helper) {
        final MachineBlockEntity compressor = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.COMPRESSOR).getFirst().get());
        for (Direction side : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            final BlockPos where = MACHINE.relative(side);
            placeBlock(helper, where, NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get());
            helper.<GeneratorBlockEntity>getBlockEntity(where).input().setItem(0, new ItemStack(Items.COAL));
        }
        insert(handler(helper, MACHINE, Direction.UP), Items.COPPER_INGOT, 1);
        final Item plate = NexusMaterials.VANILLA_PLATES.get(VanillaMetal.COPPER).get();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(compressor.slots().outputs().getItem(0).is(plate),
                        String.valueOf("no plate yet: the Compressor got no FE from the generators")))
                .thenSucceed();
    }

    private static void takesOnlyRecipes(final GameTestHelper helper) {
        furnace(helper, MACHINE, 0);

        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);

        helper.assertValueEqual(insert(top, Items.DIRT, 1), 0, String.valueOf("dirt, which nothing cooks"));
        helper.assertValueEqual(insert(top, Items.PORKCHOP, 1), 1, String.valueOf("raw pork, which cooks"));
        helper.succeed();
    }

    private static void sides(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);

        helper.assertTrue(handler(helper, MACHINE, Direction.EAST) != null,
                String.valueOf("the left side of a machine that faces north, where things go in"));
        machine.setSideMode(MachineSide.LEFT, SideMode.CLOSED);
        helper.assertTrue(handler(helper, MACHINE, Direction.EAST) == null,
                String.valueOf("the left side after it was closed"));
        helper.assertTrue(handler(helper, MACHINE, Direction.WEST) != null,
                String.valueOf("the right side, which lets things out"));
        machine.setSideMode(MachineSide.FRONT, SideMode.BOTH);
        helper.assertTrue(handler(helper, MACHINE, Direction.NORTH) != null,
                String.valueOf("the front after it was opened"));
        helper.succeed();
    }

    private static void topInBottomOut(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 0);
        machine.slots().outputs().setItem(0, new ItemStack(Items.IRON_INGOT, 3));

        helper.assertValueEqual(extract(handler(helper, MACHINE, Direction.DOWN), Items.IRON_INGOT, 2), 2,
                String.valueOf("ingots taken out at the bottom"));
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.DOWN), Items.RAW_IRON, 1), 0,
                String.valueOf("raw iron put in at the bottom, which lets things out only"));
        helper.assertValueEqual(extract(handler(helper, MACHINE, Direction.UP), Items.IRON_INGOT, 1), 0,
                String.valueOf("an ingot taken out at the top, which lets things in only"));
        helper.succeed();
    }

    private static void splits(final GameTestHelper helper) {
        final MachineBlockEntity machine = furnace(helper, MACHINE, 1);
        machine.machine().inventory().setMode(InputMode.SPLIT);

        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.RAW_IRON, STACK), STACK,
                String.valueOf("raw iron put in as a stack"));
        for (int line = 0; line < LINES_OF_ADVANCED; line++) {
            helper.assertValueEqual(machine.slots().inputs().getItem(line).getCount(), STACK / LINES_OF_ADVANCED,
                    String.valueOf("raw iron in input slot " + line));
        }
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.RAW_GOLD, 1), 0,
                String.valueOf("another resource while one is spread over the slots"));
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

        final MachineBlockEntity upgraded = helper.<MachineBlockEntity>getBlockEntity(MACHINE);
        helper.assertTrue(upgraded == machine, String.valueOf("the block entity was replaced"));
        helper.assertValueEqual(upgraded.machine().lineCount(), LINES_OF_ADVANCED, String.valueOf("lines"));
        helper.assertValueEqual(upgraded.machine().energy().stored(), (long) PER_TOP_UP,
                String.valueOf("FE kept"));
        helper.assertValueEqual(upgraded.slots().inputs().getItem(0).getCount(), 5,
                String.valueOf("raw iron kept"));
        helper.assertValueEqual(upgraded.machine().sides().mode(MachineSide.BACK), SideMode.CLOSED,
                String.valueOf("a setting kept"));
        helper.assertValueEqual(upgraded.machine().energy().capacity(), MachineTier.ADVANCED.bufferCapacity(),
                String.valueOf("buffer of the new tier"));
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
        loaded.loadCustomOnly(machine.saveCustomOnly(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());

        helper.assertValueEqual(loaded.machine().energy().stored(), (long) PER_TOP_UP, String.valueOf("FE"));
        helper.assertValueEqual(loaded.slots().inputs().getItem(1).getCount(), 7, String.valueOf("input"));
        helper.assertValueEqual(loaded.slots().outputs().getItem(2).getCount(), 4, String.valueOf("output"));
        helper.assertValueEqual(loaded.machine().inventory().mode(), InputMode.SPLIT, String.valueOf("mode"));
        helper.assertValueEqual(loaded.machine().sides().mode(MachineSide.TOP), SideMode.OUTPUT,
                String.valueOf("a side"));
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
                        String.valueOf("the machine has not started on the energy of its network")))
                .thenExecute(() -> helper.assertTrue(helper.getBlockState(ON_NETWORK).getValue(MachineBlock.PHASE)
                        != MachinePhase.OFF, String.valueOf("the machine shows no energy")))
                .thenSucceed();
    }

    private static void makes(
            final GameTestHelper helper, final MachineKind kind, final Item input, final Item result,
            final int count) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(kind).getFirst().get());
        fill(machine);
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), input, 1), 1,
                String.valueOf(input + " put in"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.slots().outputs().getItem(0).is(result),
                        String.valueOf("no " + result + " yet")))
                .thenExecute(() -> helper.assertValueEqual(machine.slots().outputs().getItem(0).getCount(), count,
                        String.valueOf("count of " + result)))
                .thenSucceed();
    }

    private static void alloys(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.ALLOY_SMELTER).getFirst().get());
        fill(machine);
        final ResourceHandler<ItemResource> top = handler(helper, MACHINE, Direction.UP);
        helper.assertValueEqual(insert(top, Items.IRON_INGOT, 1), 1, String.valueOf("iron ingot put in"));
        helper.assertValueEqual(insert(top, Items.COAL, 1), 1, String.valueOf("coal put in"));
        helper.assertValueEqual(insert(top, Items.DIRT, 1), 0, String.valueOf("dirt, which no alloy uses"));
        keepPowered(helper, machine);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        machine.slots().outputs().getItem(0).is(Items.NETHERITE_SCRAP),
                        String.valueOf("no result yet")))
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
                String.valueOf("raw iron put in through closed sides"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        access.extract(ingot, 1, Action.SIMULATE, Actor.NOBODY) == 1L,
                        String.valueOf("no ingot to take through closed sides yet")))
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
        keepPowered(helper, machine);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        machine.slots().outputs().getItem(0).is(Items.NETHERITE_SCRAP),
                        String.valueOf("no result yet")))
                .thenExecute(() -> helper.assertTrue(
                        Math.abs(helper.getLevel().getGameTime() - started - expected) <= 3,
                        String.valueOf("ticks taken " + (helper.getLevel().getGameTime() - started)
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
        keepPowered(helper, machine);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machine.getUpdateTag(helper.getLevel().registryAccess())
                        .getBoolean("cycle_active"), String.valueOf("cycle not sent")))
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
                        && machine.shownItem(1).is(Items.COAL), String.valueOf("inputs not shown")))
                .thenWaitUntil(() -> helper.assertTrue(machine.shownItem(3).is(Items.NETHERITE_SCRAP),
                        String.valueOf("result not shown while it works")))
                .thenSucceed();
    }

    private static void extracts(final GameTestHelper helper) {
        final MachineBlockEntity machine = placeMachine(helper, MACHINE,
                NexusBlocks.machineTiers(MachineKind.EXTRACTOR).getFirst().get());
        fill(machine);
        helper.assertValueEqual(insert(handler(helper, MACHINE, Direction.UP), Items.ICE, 1), 1,
                String.valueOf("ice put in"));
        final ResourceHandler<FluidResource> tank = ResourceHandler.ofFluids(helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(MACHINE), Direction.DOWN));
        helper.assertTrue(tank != null, String.valueOf("the bottom of an Extractor gives no fluid"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(tank.getAmountAsInt(0), ICE_MILLIBUCKETS,
                        String.valueOf("millibuckets in the tank")))
                .thenExecute(() -> helper.assertTrue(tank.getResource(0).getFluid() == Fluids.WATER,
                        String.valueOf("the fluid in the tank")))
                .thenExecute(() -> helper.assertValueEqual(putIntoTank(tank), 0,
                        String.valueOf("water put into the tank from outside")))
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
        return helper.<MachineBlockEntity>getBlockEntity(pos);
    }

    /**
     * Tops the machine up every tick, as the network does for one in play: a recipe of the Alloy Smelter costs more FE
     * than its buffer holds.
     */
    private static void keepPowered(final GameTestHelper helper, final MachineBlockEntity machine) {
        helper.onEachTick(() -> machine.machine().energy().insert(PER_TOP_UP, Action.EXECUTE));
    }

    private static void fill(final MachineBlockEntity machine) {
        for (int top = 0; top < BUFFER_TOP_UPS; top++) {
            machine.machine().energy().insert(PER_TOP_UP, Action.EXECUTE);
        }
    }

    private static @Nullable ResourceHandler<ItemResource> handler(
            final GameTestHelper helper, final BlockPos pos, final Direction side) {
        return ResourceHandler.ofItems(
                helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side));
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
