package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.NetworkDeviceBlock;
import com.morphengine.nexus.block.NexusBlock;
import com.morphengine.nexus.block.NexusStatus;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.processing.MachinePhase;
import com.morphengine.nexus.registry.NexusBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class NexusGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final long CELL_CAPACITY = EnergyCellTier.BASIC.capacity();
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    /** Long enough for the network to be rebuilt and its statistics refreshed twice. */
    private static final int SETTLE_TICKS = 45;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("cable_run_joins_energy_cell", NexusGameTests::cableRunJoinsEnergyCell),
            Map.entry("broken_cable_splits_network", NexusGameTests::brokenCableSplitsNetwork),
            Map.entry("different_colors_do_not_join", NexusGameTests::differentColorsDoNotJoin),
            Map.entry("device_bridges_colors", NexusGameTests::deviceBridgesColors),
            Map.entry("nexus_opens_port_only_for_cable", NexusGameTests::nexusOpensPortOnlyForCable),
            Map.entry("external_energy_reaches_pool", NexusGameTests::externalEnergyReachesPool),
            Map.entry("aborted_transaction_leaves_cell_unchanged",
                    NexusGameTests::abortedTransactionLeavesCellUnchanged),
            Map.entry("cell_knows_its_network", NexusGameTests::cellKnowsItsNetwork),
            Map.entry("coal_generator_charges_cell", NexusGameTests::coalGeneratorChargesCell),
            Map.entry("devices_show_network_color", NexusGameTests::devicesShowNetworkColor),
            Map.entry("cut_off_device_shows_unconnected_color", NexusGameTests::cutOffDeviceShowsUnconnectedColor),
            Map.entry("generator_feeds_network", NexusGameTests::generatorFeedsNetwork),
            Map.entry("generator_front_takes_no_cable", NexusGameTests::generatorFrontTakesNoCable),
            Map.entry("second_nexus_stands_down", NexusGameTests::secondNexusStandsDown),
            Map.entry("cables_light_up_with_energy", NexusGameTests::cablesLightUpWithEnergy));

    private NexusGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(NexusGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void cableRunJoinsEnergyCell(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cable(DyeColor.BLUE), cable(DyeColor.BLUE), cell());

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenSucceed();
    }

    private static void brokenCableSplitsNetwork(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cable(DyeColor.BLUE), cable(DyeColor.BLUE), cell());

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> helper.destroyBlock(NEXUS.east(2)))
                .thenWaitUntil(() -> assertStatistics(helper, 0, 0))
                .thenSucceed();
    }

    private static void differentColorsDoNotJoin(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cable(DyeColor.RED), cell());

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> assertStatistics(helper, 0, 0))
                .thenExecute(() -> assertAttached(helper, NEXUS.east(), Direction.EAST, false))
                .thenSucceed();
    }

    private static void deviceBridgesColors(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cell(), cable(DyeColor.RED), cell());

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 2, 2 * CELL_CAPACITY))
                .thenSucceed();
    }

    private static void nexusOpensPortOnlyForCable(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, NEXUS.east(), cable(DyeColor.BLUE));
        place(helper, NEXUS.south(), cell());

        helper.startSequence()
                .thenExecute(() -> {
                    assertAttached(helper, NEXUS, Direction.EAST, true);
                    assertAttached(helper, NEXUS, Direction.SOUTH, false);
                    assertAttached(helper, NEXUS.east(), Direction.WEST, true);
                })
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenSucceed();
    }

    private static void externalEnergyReachesPool(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cell());
        final BlockPos cellPos = NEXUS.east(2);

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        final int accepted = TestEnergy.handler(helper, cellPos).insert(500, transaction);
                        helper.assertTrue(accepted == 500, String.valueOf("cell accepted " + accepted + " of 500"));
                        transaction.commit();
                    }
                })
                .thenWaitUntil(() -> {
                    final long stored = nexus(helper).statistics().energyStored();
                    helper.assertTrue(stored == 500, String.valueOf("pool stores " + stored + ", expected 500"));
                })
                .thenSucceed();
    }

    private static void abortedTransactionLeavesCellUnchanged(final GameTestHelper helper) {
        final BlockPos cellPos = new BlockPos(1, 1, 1);
        place(helper, cellPos, cell());

        helper.startSequence()
                .thenExecute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        TestEnergy.handler(helper, cellPos).insert(500, transaction);
                    }
                    final long stored = cellEntity(helper, cellPos).energyBuffer().stored();
                    helper.assertTrue(stored == 0, String.valueOf("aborted insert left " + stored + " FE"));
                })
                .thenSucceed();
    }

    private static void cellKnowsItsNetwork(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cell());
        final BlockPos cellPos = NEXUS.east(2);
        final NetworkColor red = new NetworkColor(0xB02E26);

        helper.startSequence()
                .thenExecute(() -> nexus(helper).recolor(red))
                .thenWaitUntil(() -> {
                    final NetworkBadge badge = cellEntity(helper, cellPos).networkBadge();
                    helper.assertTrue(badge != null && badge.color().equals(red),
                            String.valueOf("cell sees network " + badge));
                })
                .thenExecute(() -> helper.destroyBlock(NEXUS.east()))
                .thenWaitUntil(() -> helper.assertTrue(cellEntity(helper, cellPos).networkBadge() == null,
                        String.valueOf("cell still sees a network after its cable was broken")))
                .thenSucceed();
    }

    private static void coalGeneratorChargesCell(final GameTestHelper helper) {
        final BlockPos generatorPos = new BlockPos(3, 1, 3);
        final BlockPos cellPos = generatorPos.east();
        place(helper, generatorPos, NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get().defaultBlockState());
        place(helper, cellPos, cell());

        helper.startSequence()
                .thenExecute(() -> helper.<GeneratorBlockEntity>getBlockEntity(generatorPos)
                        .input().setItem(0, new ItemStack(Items.CHARCOAL, 2)))
                .thenWaitUntil(() -> {
                    final long stored = cellEntity(helper, cellPos).energyBuffer().stored();
                    final boolean lit =
                            helper.getBlockState(generatorPos).getValue(GeneratorBlock.PHASE) == MachinePhase.ACTIVE;
                    helper.assertTrue(stored > 0 && lit,
                            String.valueOf("cell holds " + stored + " FE, generator lit=" + lit));
                })
                .thenSucceed();
    }

    private static void devicesShowNetworkColor(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cell());
        final BlockPos cellPos = NEXUS.east(2);

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> nexus(helper).recolor(NetworkColoring.colorOf(DyeColor.RED)))
                .thenWaitUntil(() -> {
                    assertNetworkColor(helper, NEXUS, DyeColor.RED);
                    assertNetworkColor(helper, cellPos, DyeColor.RED);
                })
                .thenSucceed();
    }

    private static void cutOffDeviceShowsUnconnectedColor(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cell());
        final BlockPos cellPos = NEXUS.east(2);

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> nexus(helper).recolor(NetworkColoring.colorOf(DyeColor.GREEN)))
                .thenWaitUntil(() -> assertNetworkColor(helper, cellPos, DyeColor.GREEN))
                .thenExecute(() -> helper.destroyBlock(NEXUS.east()))
                .thenWaitUntil(() -> {
                    assertNetworkColor(helper, cellPos, NetworkColoring.UNCONNECTED);
                    assertNetworkColor(helper, NEXUS, DyeColor.GREEN);
                })
                .thenSucceed();
    }

    private static void generatorFeedsNetwork(final GameTestHelper helper) {
        final BlockPos cellPos = NEXUS.south();
        final BlockPos generatorPos = NEXUS.east(2);
        buildLine(helper, cable(DyeColor.BLUE), generator(Direction.NORTH));
        place(helper, cellPos, cell());

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 2, CELL_CAPACITY))
                .thenExecute(() -> helper.<GeneratorBlockEntity>getBlockEntity(generatorPos)
                        .input().setItem(0, new ItemStack(Items.CHARCOAL, 1)))
                .thenWaitUntil(() -> {
                    final long stored = cellEntity(helper, cellPos).energyBuffer().stored();
                    helper.assertTrue(stored > 0, String.valueOf("cell away from the generator holds no FE"));
                })
                .thenSucceed();
    }

    private static void generatorFrontTakesNoCable(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), generator(Direction.WEST));

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    assertAttached(helper, NEXUS.east(), Direction.EAST, false);
                    assertStatistics(helper, 0, 0);
                })
                .thenSucceed();
    }

    private static void secondNexusStandsDown(final GameTestHelper helper) {
        final BlockPos second = NEXUS.east(3);
        buildLine(helper, cable(DyeColor.BLUE), cell());

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> place(helper, second, NexusBlocks.NEXUS.get().defaultBlockState()))
                .thenWaitUntil(() -> assertStatus(helper, second, NexusStatus.CONFLICT))
                .thenExecute(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> helper.destroyBlock(NEXUS))
                .thenWaitUntil(() -> {
                    final NetworkStatistics statistics =
                            helper.<NexusBlockEntity>getBlockEntity(second).statistics();
                    helper.assertTrue(statistics.devices() == 1,
                            String.valueOf("remaining Nexus leads " + statistics.devices() + " devices"));
                    assertStatus(helper, second, NexusStatus.NO_ENERGY);
                })
                .thenSucceed();
    }

    private static void cablesLightUpWithEnergy(final GameTestHelper helper) {
        buildLine(helper, cable(DyeColor.BLUE), cell());
        final BlockPos cablePos = NEXUS.east();

        helper.startSequence()
                .thenWaitUntil(() -> assertStatistics(helper, 1, CELL_CAPACITY))
                .thenExecute(() -> assertPowered(helper, cablePos, false))
                .thenExecute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        TestEnergy.handler(helper, NEXUS.east(2)).insert(500, transaction);
                        transaction.commit();
                    }
                })
                .thenWaitUntil(() -> {
                    assertPowered(helper, cablePos, true);
                    assertStatus(helper, NEXUS, NexusStatus.ONLINE);
                })
                .thenSucceed();
    }

    private static void assertStatus(final GameTestHelper helper, final BlockPos pos, final NexusStatus expected) {
        final NexusStatus shown = helper.getBlockState(pos).getValue(NexusBlock.STATUS);
        helper.assertTrue(shown == expected,
                String.valueOf("Nexus at " + pos + " shows " + shown + ", expected " + expected));
    }

    private static void assertPowered(final GameTestHelper helper, final BlockPos pos, final boolean expected) {
        final boolean powered = helper.getBlockState(pos).getValue(CableBlock.POWERED);
        helper.assertTrue(powered == expected, String.valueOf("cable at " + pos + " powered=" + powered));
    }

    private static BlockState generator(final Direction facing) {
        return NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get().defaultBlockState()
                .setValue(GeneratorBlock.FACING, facing);
    }

    private static void assertNetworkColor(final GameTestHelper helper, final BlockPos pos, final DyeColor expected) {
        final DyeColor shown = helper.getBlockState(pos).getValue(NetworkDeviceBlock.NETWORK_COLOR);
        helper.assertTrue(shown == expected,
                String.valueOf("device at " + pos + " shows " + shown + ", expected " + expected));
    }

    private static EnergyCellBlockEntity cellEntity(final GameTestHelper helper, final BlockPos pos) {
        return helper.<EnergyCellBlockEntity>getBlockEntity(pos);
    }

    private static void buildLine(final GameTestHelper helper, final BlockState... eastOfNexus) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        for (int i = 0; i < eastOfNexus.length; i++) {
            place(helper, NEXUS.east(i + 1), eastOfNexus[i]);
        }
    }

    /**
     * Places a block the way a player would: its sides are worked out from the
     * neighbours already there, which a bare {@code setBlock} skips.
     */
    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }

    private static BlockState cable(final DyeColor color) {
        return NexusBlocks.CABLES.get(color).get().defaultBlockState();
    }

    private static BlockState cell() {
        return NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState();
    }

    private static NexusBlockEntity nexus(final GameTestHelper helper) {
        return helper.<NexusBlockEntity>getBlockEntity(NEXUS);
    }

    private static void assertStatistics(final GameTestHelper helper, final int devices, final long capacity) {
        final NetworkStatistics statistics = nexus(helper).statistics();
        helper.assertTrue(statistics.devices() == devices && statistics.energyCapacity() == capacity,
                String.valueOf("expected " + devices + " devices and " + capacity + " FE capacity, got "
                        + statistics));
    }

    private static void assertAttached(
            final GameTestHelper helper, final BlockPos pos, final Direction side, final boolean expected) {
        final BlockState state = helper.getBlockState(pos);
        final boolean attached = state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
        helper.assertTrue(attached == expected,
                String.valueOf(state.getBlock().getName().getString() + " " + side + " attached=" + attached));
    }
}
