package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An Energy Cell charges the item in its charging slot from its own buffer, through the energy capability of the
 * item, and a Nexus Terminal is such an item.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ItemChargingGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 100;
    private static final int CELL_CHARGE = 5_000;
    private static final int ROOM_LEFT = 100;
    private static final BlockPos CELL = new BlockPos(1, 1, 1);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("cell_charges_item_in_its_slot", ItemChargingGameTests::chargesItem),
            Map.entry("cell_charges_item_only_to_full", ItemChargingGameTests::stopsWhenFull),
            Map.entry("cell_leaves_item_that_stores_no_energy", ItemChargingGameTests::leavesOtherItems),
            Map.entry("terminal_gives_no_energy_back", ItemChargingGameTests::terminalGivesNothingBack));

    private ItemChargingGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(ItemChargingGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void chargesItem(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = chargedCell(helper);
        final ItemStack terminal = new ItemStack(NexusItems.NEXUS_TERMINAL.get());
        cell.chargingSlot().setItem(0, terminal);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heldCharge(cell) > 0,
                        String.valueOf("the terminal in the charging slot took no charge")))
                .thenExecute(() -> helper.assertValueEqual(
                        heldCharge(cell) + cell.energyBuffer().stored(), (long) CELL_CHARGE,
                        String.valueOf("FE of the terminal and the cell together")))
                .thenSucceed();
    }

    private static void stopsWhenFull(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = chargedCell(helper);
        final ItemStack terminal = new ItemStack(NexusItems.NEXUS_TERMINAL.get());
        terminal.set(NexusDataComponents.TERMINAL_CHARGE.get(), NexusTerminalItem.capacity() - ROOM_LEFT);
        cell.chargingSlot().setItem(0, terminal);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(heldCharge(cell), (long) NexusTerminalItem.capacity(),
                        String.valueOf("charge of the terminal")))
                .thenIdle(5)
                .thenExecute(() -> helper.assertValueEqual(cell.energyBuffer().stored(),
                        (long) CELL_CHARGE - ROOM_LEFT, String.valueOf("FE left in the cell")))
                .thenSucceed();
    }

    private static void leavesOtherItems(final GameTestHelper helper) {
        final EnergyCellBlockEntity cell = chargedCell(helper);
        cell.chargingSlot().setItem(0, new ItemStack(Items.STICK));

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertValueEqual(cell.energyBuffer().stored(), (long) CELL_CHARGE,
                        String.valueOf("FE left in the cell")))
                .thenSucceed();
    }

    private static void terminalGivesNothingBack(final GameTestHelper helper) {
        final ItemStack terminal = new ItemStack(NexusItems.NEXUS_TERMINAL.get());
        terminal.set(NexusDataComponents.TERMINAL_CHARGE.get(), NexusTerminalItem.openCost());
        final EnergyHandler handler = EnergyHandler.of(terminal.getCapability(Capabilities.EnergyStorage.ITEM));

        helper.assertTrue(handler != null, String.valueOf("a terminal has no energy capability"));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(handler.extract(NexusTerminalItem.openCost(), transaction), 0,
                    String.valueOf("FE a charger took out of the terminal"));
        }
        helper.succeed();
    }

    private static long heldCharge(final EnergyCellBlockEntity cell) {
        return NexusTerminalItem.chargeOf(cell.chargingSlot().getItem(0));
    }

    private static EnergyCellBlockEntity chargedCell(final GameTestHelper helper) {
        helper.setBlock(CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        TestEnergy.fill(helper, CELL, CELL_CHARGE);
        return helper.<EnergyCellBlockEntity>getBlockEntity(CELL);
    }
}
