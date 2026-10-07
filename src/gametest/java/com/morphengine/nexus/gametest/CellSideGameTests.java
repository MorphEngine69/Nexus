package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.menu.EnergyCellMenu;
import com.morphengine.nexus.registry.NexusBlocks;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * The sides of an Energy Cell: closed to blocks of other mods until the player opens one, and then open as its mode
 * says. A block of a network is not held back by a closed side.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class CellSideGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
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
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "cell_sides"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void inputOnly(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(SIDE, SideMode.INPUT);

        final EnergyHandler handler = handlerAt(helper, SIDE);

        helper.assertTrue(handler != null, Component.literal("an input side is closed"));
        helper.assertValueEqual(insert(handler, OFFERED), OFFERED, Component.literal("FE put in through an input"));
        helper.assertValueEqual(extract(handler, OFFERED), 0, Component.literal("FE taken out through an input"));
        helper.succeed();
    }

    private static void outputOnly(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(SIDE, SideMode.OUTPUT);

        final EnergyHandler handler = handlerAt(helper, SIDE);

        helper.assertTrue(handler != null, Component.literal("an output side is closed"));
        helper.assertValueEqual(extract(handler, OFFERED), OFFERED,
                Component.literal("FE taken out through an output"));
        helper.assertValueEqual(insert(handler, OFFERED), 0, Component.literal("FE put in through an output"));
        helper.succeed();
    }

    private static void both(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(SIDE, SideMode.BOTH);

        final EnergyHandler handler = handlerAt(helper, SIDE);

        helper.assertTrue(handler != null, Component.literal("a side that does both is closed"));
        helper.assertValueEqual(extract(handler, OFFERED), OFFERED, Component.literal("FE taken out"));
        helper.assertValueEqual(insert(handler, OFFERED), OFFERED, Component.literal("FE put in"));
        helper.succeed();
    }

    private static void closedAgain(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        final BlockCapabilityCache<EnergyHandler, Direction> asked = BlockCapabilityCache.create(
                Capabilities.Energy.BLOCK, helper.getLevel(), helper.absolutePos(CELL), SIDE);
        helper.assertTrue(asked.getCapability() == null, Component.literal("a side starts open"));

        helper.startSequence()
                .thenExecute(() -> cell.setSideMode(SIDE, SideMode.BOTH))
                .thenExecute(() -> helper.assertTrue(asked.getCapability() != null,
                        Component.literal("a block that asked before does not see the opened side")))
                .thenExecute(() -> cell.setSideMode(SIDE, SideMode.CLOSED))
                .thenExecute(() -> helper.assertTrue(asked.getCapability() == null,
                        Component.literal("a block that asked before still sees the closed side")))
                .thenSucceed();
    }

    private static void oneAtATime(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);

        cell.setSideMode(Direction.EAST, SideMode.BOTH);

        for (Direction side : Direction.values()) {
            helper.assertTrue((handlerAt(helper, side) != null) == (side == Direction.EAST),
                    Component.literal("the side " + side + " is " + (side == Direction.EAST ? "closed" : "open")));
        }
        helper.succeed();
    }

    private static void survivesSavingAndUpgrade(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        cell.setSideMode(Direction.EAST, SideMode.INPUT);
        cell.setSideMode(Direction.UP, SideMode.BOTH);

        final EnergyCellBlockEntity loaded = new EnergyCellBlockEntity(
                helper.absolutePos(CELL), helper.getBlockState(CELL));
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(),
                cell.saveCustomOnly(helper.getLevel().registryAccess())));
        helper.assertValueEqual(loaded.sideBits(), cell.sideBits(), Component.literal("sides after loading"));

        final BlockState before = helper.getBlockState(CELL);
        helper.setBlock(CELL, NexusBlocks.ADVANCED_ENERGY_CELL.get().withPropertiesOf(before));
        final EnergyCellBlockEntity upgraded = helper.getBlockEntity(CELL, EnergyCellBlockEntity.class);
        helper.assertValueEqual(upgraded.sideMode(Direction.EAST), SideMode.INPUT, Component.literal("EAST kept"));
        helper.assertValueEqual(upgraded.sideMode(Direction.UP), SideMode.BOTH, Component.literal("UP kept"));
        helper.succeed();
    }

    private static void panelButtons(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = cellWithForeignNeighbours(helper);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        final EnergyCellMenu menu = new EnergyCellMenu(1, player.getInventory(), helper.absolutePos(CELL));

        menu.clickMenuButton(player, EnergyCellMenu.BUTTON_SIDE_NEXT + SIDE.ordinal());
        helper.assertValueEqual(cell.sideMode(SIDE), SideMode.INPUT, Component.literal("a click forward"));
        menu.clickMenuButton(player, EnergyCellMenu.BUTTON_SIDE_PREVIOUS + SIDE.ordinal());
        menu.clickMenuButton(player, EnergyCellMenu.BUTTON_SIDE_PREVIOUS + SIDE.ordinal());
        helper.assertValueEqual(cell.sideMode(SIDE), SideMode.BOTH, Component.literal("two clicks back from input"));
        helper.assertValueEqual(cell.sideMode(Direction.SOUTH), SideMode.CLOSED, Component.literal("another side"));
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
        final EnergyCellBlockEntity cell = helper.getBlockEntity(CELL, EnergyCellBlockEntity.class);
        cell.energyBuffer().insert(STORED, Action.EXECUTE);
        return cell;
    }

    private static @Nullable EnergyHandler handlerAt(final GameTestHelper helper, final Direction side) {
        return helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(CELL), side);
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
