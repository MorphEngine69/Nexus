package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The sides of an Energy Cell: closed to blocks of other mods until the player opens one, and then open as its mode
 * says. A block of a network is not held back by a closed side.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class CellSideGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos CELL = new BlockPos(5, 1, 6);
    private static final Direction SIDE = Direction.NORTH;
    private static final int STORED = 600;
    private static final int OFFERED = 100;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("cell_side_set_to_input_lets_another_mod_put_energy_in_only", CellSideGameTests::inputOnly),
            Map.entry("cell_side_set_to_output_lets_another_mod_take_energy_out_only", CellSideGameTests::outputOnly),
            Map.entry("cell_side_set_to_both_lets_another_mod_do_both", CellSideGameTests::both),
            Map.entry("cell_side_closed_again_shuts_out_a_block_that_asked_before", CellSideGameTests::closedAgain),
            Map.entry("cell_sides_open_one_at_a_time", CellSideGameTests::oneAtATime),
            Map.entry("cell_sides_survive_saving_and_a_tier_upgrade", CellSideGameTests::survivesSavingAndUpgrade),
            Map.entry("cell_panel_buttons_move_a_side_forward_and_back", CellSideGameTests::panelButtons));

    private CellSideGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(CellSideGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void inputOnly(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(SIDE, SideMode.INPUT);

        final EnergyHandler handler = handlerAt(helper, SIDE);

        helper.assertTrue(handler != null, String.valueOf("an input side is closed"));
        helper.assertValueEqual(insert(handler, OFFERED), OFFERED, String.valueOf("FE put in through an input"));
        helper.assertValueEqual(extract(handler, OFFERED), 0, String.valueOf("FE taken out through an input"));
        helper.succeed();
    }

    private static void outputOnly(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(SIDE, SideMode.OUTPUT);

        final EnergyHandler handler = handlerAt(helper, SIDE);

        helper.assertTrue(handler != null, String.valueOf("an output side is closed"));
        helper.assertValueEqual(extract(handler, OFFERED), OFFERED,
                String.valueOf("FE taken out through an output"));
        helper.assertValueEqual(insert(handler, OFFERED), 0, String.valueOf("FE put in through an output"));
        helper.succeed();
    }

    private static void both(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(SIDE, SideMode.BOTH);

        final EnergyHandler handler = handlerAt(helper, SIDE);

        helper.assertTrue(handler != null, String.valueOf("a side that does both is closed"));
        helper.assertValueEqual(extract(handler, OFFERED), OFFERED, String.valueOf("FE taken out"));
        helper.assertValueEqual(insert(handler, OFFERED), OFFERED, String.valueOf("FE put in"));
        helper.succeed();
    }

    private static void closedAgain(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        final BlockCapabilityCache<IEnergyStorage, Direction> asked = BlockCapabilityCache.create(
                Capabilities.EnergyStorage.BLOCK, helper.getLevel(), helper.absolutePos(CELL), SIDE);
        helper.assertTrue(asked.getCapability() == null, String.valueOf("a side starts open"));

        helper.startSequence()
                .thenExecute(() -> cell.setSideMode(SIDE, SideMode.BOTH))
                .thenExecute(() -> helper.assertTrue(asked.getCapability() != null,
                        String.valueOf("a block that asked before does not see the opened side")))
                .thenExecute(() -> cell.setSideMode(SIDE, SideMode.CLOSED))
                .thenExecute(() -> helper.assertTrue(asked.getCapability() == null,
                        String.valueOf("a block that asked before still sees the closed side")))
                .thenSucceed();
    }

    private static void oneAtATime(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);

        cell.setSideMode(Direction.EAST, SideMode.BOTH);

        for (Direction side : Direction.values()) {
            helper.assertTrue((handlerAt(helper, side) != null) == (side == Direction.EAST),
                    String.valueOf("the side " + side + " is " + (side == Direction.EAST ? "closed" : "open")));
        }
        helper.succeed();
    }

    private static void survivesSavingAndUpgrade(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(Direction.EAST, SideMode.INPUT);
        cell.setSideMode(Direction.UP, SideMode.BOTH);

        final EnergyCellBlockEntity loaded = new EnergyCellBlockEntity(
                helper.absolutePos(CELL), helper.getBlockState(CELL));
        loaded.loadCustomOnly(cell.saveCustomOnly(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        helper.assertValueEqual(loaded.sideBits(), cell.sideBits(), String.valueOf("sides after loading"));

        final BlockState before = helper.getBlockState(CELL);
        helper.setBlock(CELL, NexusBlocks.ADVANCED_ENERGY_CELL.get().withPropertiesOf(before));
        final EnergyCellBlockEntity upgraded = helper.<EnergyCellBlockEntity>getBlockEntity(CELL);
        helper.assertValueEqual(upgraded.sideMode(Direction.EAST), SideMode.INPUT, String.valueOf("EAST kept"));
        helper.assertValueEqual(upgraded.sideMode(Direction.UP), SideMode.BOTH, String.valueOf("UP kept"));
        helper.succeed();
    }

    private static void panelButtons(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        final EnergyCellMenu menu = new EnergyCellMenu(1, player.getInventory(), helper.absolutePos(CELL));

        menu.clickMenuButton(player, EnergyCellMenu.BUTTON_SIDE_NEXT + SIDE.ordinal());
        helper.assertValueEqual(cell.sideMode(SIDE), SideMode.INPUT, String.valueOf("a click forward"));
        menu.clickMenuButton(player, EnergyCellMenu.BUTTON_SIDE_PREVIOUS + SIDE.ordinal());
        menu.clickMenuButton(player, EnergyCellMenu.BUTTON_SIDE_PREVIOUS + SIDE.ordinal());
        helper.assertValueEqual(cell.sideMode(SIDE), SideMode.BOTH, String.valueOf("two clicks back from input"));
        helper.assertValueEqual(cell.sideMode(Direction.SOUTH), SideMode.CLOSED, String.valueOf("another side"));
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    /**
     * A cell with some energy and a stone against every side, so that no block of a network stands there.
     */
    private static EnergyCellBlockEntity cellWithForeignNeighbours(final GameTestHelper helper) {
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        for (Direction side : Direction.values()) {
            place(helper, CELL.relative(side), Blocks.STONE.defaultBlockState());
        }
        final EnergyCellBlockEntity cell = helper.<EnergyCellBlockEntity>getBlockEntity(CELL);
        cell.energyBuffer().insert(STORED, Action.EXECUTE);
        return cell;
    }

    private static @Nullable EnergyHandler handlerAt(final GameTestHelper helper, final Direction side) {
        return EnergyHandler.of(
                helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(CELL), side));
    }

    /**
     * @return what the handler would take; a transaction that is not committed changes nothing
     */
    private static int insert(final EnergyHandler handler, final int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int accepted = handler.insert(amount, transaction);
            return accepted;
        }
    }

    private static int extract(final EnergyHandler handler, final int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int removed = handler.extract(amount, transaction);
            return removed;
        }
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
