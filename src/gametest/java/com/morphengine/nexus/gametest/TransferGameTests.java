package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.energy.EnergyBuffer;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.api.resource.FilterMatchMode;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.EnergyCellBlock;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.NetworkColoring;
import com.morphengine.nexus.block.NetworkDeviceBlock;
import com.morphengine.nexus.block.SideConnections;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.energy.OperationKind;
import com.morphengine.nexus.energy.OperationPrice;
import com.morphengine.nexus.energy.OperationUpgrades;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.OperationToll;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.transfer.DeliveryMode;
import com.morphengine.nexus.transfer.FluidResource;
import com.morphengine.nexus.transfer.TransferKind;
import com.morphengine.nexus.transfer.TransferResource;
import com.morphengine.nexus.transfer.TransferSettings;
import com.morphengine.nexus.transfer.WorldMode;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Attached devices in a world: Pullers and Pushers work with the block their
 * face touches as that block offers itself on that face, keep amounts stocked
 * and follow their redstone mode; Placers and Removers place, drop, break and
 * pick up in the space their face touches.
 *
 * <p>Every test starts from the same network along the north edge: a Nexus, a
 * charged Energy Cell south of it, a Storage Vault with a cell east of it and a
 * cable east of the vault.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class TransferGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final int NETWORK_FILL = 10_000;
    private static final int STONES_TO_MOVE = 3;
    private static final int PAST_THE_LAST_OPERATION_TICKS = 40;
    /** Inserts of one maximum transfer that fill a basic Energy Cell to sixty percent. */
    private static final int SIXTY_PERCENT_STEPS = 60;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos TARGET = CABLE.east();
    /** Where a Placer or Remover stands, facing east. */
    private static final BlockPos DEVICE = CABLE.above();
    private static final BlockPos FRONT = DEVICE.east();

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("puller_empties_chest_into_network", TransferGameTests::pullerEmptiesChest),
            Map.entry("pusher_beside_furnace_fills_only_fuel", TransferGameTests::pusherBesideFurnaceFillsFuel),
            Map.entry("puller_under_furnace_takes_only_result", TransferGameTests::pullerUnderFurnaceTakesResult),
            Map.entry("pusher_keeps_target_stocked", TransferGameTests::pusherKeepsTargetStocked),
            Map.entry("puller_on_high_signal_waits_for_signal", TransferGameTests::pullerWaitsForSignal),
            Map.entry("pusher_with_blacklist_delivers_everything_else", TransferGameTests::pusherBlacklist),
            Map.entry("nexus_counts_pullers_pushers_and_storages", TransferGameTests::nexusCountsRoles),
            Map.entry("device_arms_glow_with_energy", TransferGameTests::deviceArmsGlow),
            Map.entry("speed_upgrades_make_puller_work_more_often", TransferGameTests::speedUpgradesMakePullerFaster),
            Map.entry("puller_pays_for_every_operation_that_moves_something", TransferGameTests::pullerPaysForWork),
            Map.entry("puller_with_nothing_to_move_costs_nothing", TransferGameTests::idlePullerCostsNothing),
            Map.entry("puller_stands_still_when_the_network_cannot_pay", TransferGameTests::pullerNeedsEnergyToWork),
            Map.entry("a_price_above_what_a_cell_gives_in_one_go_is_still_paid", TransferGameTests::dearPriceIsPaid),
            Map.entry("stack_upgrade_moves_a_stack_at_once", TransferGameTests::stackUpgradeMovesStack),
            Map.entry("pusher_without_regulator_ignores_amount", TransferGameTests::pusherWithoutRegulator),
            Map.entry("puller_with_regulator_leaves_stock", TransferGameTests::pullerWithRegulatorLeavesStock),
            Map.entry("filters_survive_switching_resource", TransferGameTests::filtersSurviveResourceSwitch),
            Map.entry("device_takes_only_its_upgrades", TransferGameTests::deviceTakesOnlyItsUpgrades),
            Map.entry("speed_upgrades_share_one_slot", TransferGameTests::speedUpgradesShareOneSlot),
            Map.entry("capacity_upgrades_add_filter_slots", TransferGameTests::capacityUpgradesAddFilterSlots),
            Map.entry("match_mode_normalizes_item_keys", TransferGameTests::matchModeNormalizesItemKeys),
            Map.entry("pusher_blacklist_ignores_durability_with_match_mode",
                    TransferGameTests::pusherBlacklistIgnoresDurabilityWithMatchMode),
            Map.entry("placer_places_listed_block", TransferGameTests::placerPlacesBlock),
            Map.entry("placer_drops_items_in_item_mode", TransferGameTests::placerDropsItems),
            Map.entry("placer_pours_fluid_source", TransferGameTests::placerPoursFluid),
            Map.entry("remover_breaks_block_into_network", TransferGameTests::removerBreaksBlock),
            Map.entry("remover_with_silk_touch_keeps_block_whole", TransferGameTests::removerWithSilkTouch),
            Map.entry("remover_whitelist_leaves_other_blocks", TransferGameTests::removerWhitelist),
            Map.entry("remover_picks_up_items_in_item_mode", TransferGameTests::removerPicksUpItems),
            Map.entry("remover_takes_fluid_source", TransferGameTests::removerTakesFluid),
            Map.entry("puller_with_tag_takes_only_members", TransferGameTests::pullerWithTag),
            Map.entry("pusher_with_tag_delivers_members", TransferGameTests::pusherWithTag),
            Map.entry("filter_tag_survives_saving", TransferGameTests::filterTagSurvivesSaving),
            Map.entry("devices_take_cables_on_every_side_but_their_face",
                    TransferGameTests::devicesTakeCablesOnEverySideButTheirFace),
            Map.entry("transfer_devices_show_network_color", TransferGameTests::devicesShowNetworkColor),
            Map.entry("assembler_takes_cables_on_every_side_but_its_face",
                    TransferGameTests::assemblerTakesCablesOnEverySideButItsFace),
            Map.entry("assembler_shows_network_color", TransferGameTests::assemblerShowsNetworkColor),
            Map.entry("generator_takes_cables_on_every_side_but_its_front",
                    TransferGameTests::generatorTakesCablesOnEverySideButItsFront),
            Map.entry("energy_cell_shows_its_charge_level", TransferGameTests::energyCellShowsItsChargeLevel));

    private TransferGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(TransferGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void pullerEmptiesChest(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        container(helper, chest).setItem(0, new ItemStack(Items.STONE, 5));
        place(helper, CABLE.above(), device(NexusBlocks.PULLER.get(), Direction.EAST));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 5,
                        "stone in the network"))
                .thenExecute(() -> helper.assertTrue(container(helper, chest).isEmpty(),
                        String.valueOf("the chest still holds stone")))
                .thenSucceed();
    }

    private static void pusherBesideFurnaceFillsFuel(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, TARGET, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        place(helper, CABLE.above(), cable());
        place(helper, TARGET.above(), cable());
        place(helper, TARGET.above().east(), cable());
        final BlockPos pusher = TARGET.east();
        place(helper, pusher, device(NexusBlocks.PUSHER.get(), Direction.WEST));
        deviceEntity(helper, pusher).changeSettings(TransferSettings.DEFAULT.withFilter(
                FilterSlots.EMPTY.with(0, key(Items.IRON_ORE)).with(1, key(Items.COAL))));

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.IRON_ORE), 4))
                .thenExecute(() -> assertInserted(helper, key(Items.COAL), 4))
                .thenWaitUntil(() -> assertCount(helper, container(helper, TARGET).getItem(1), Items.COAL, 4))
                .thenExecute(() -> helper.assertTrue(container(helper, TARGET).getItem(0).isEmpty(),
                        String.valueOf("the furnace took ore through its side")))
                .thenSucceed();
    }

    private static void pullerUnderFurnaceTakesResult(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos puller = TARGET;
        final BlockPos furnace = TARGET.above();
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.UP));
        place(helper, furnace, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        container(helper, furnace).setItem(1, new ItemStack(Items.COAL, 3));
        container(helper, furnace).setItem(2, new ItemStack(Items.IRON_INGOT, 2));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.IRON_INGOT)), 2,
                        "iron ingots in the network"))
                .thenIdle(20)
                .thenExecute(() -> assertCount(helper, container(helper, furnace).getItem(1), Items.COAL, 3))
                .thenSucceed();
    }

    private static void pusherKeepsTargetStocked(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos pusher = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        place(helper, pusher, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, pusher).changeSettings(keepingStone(3));
        install(helper, pusher, NexusItems.REGULATOR_UPGRADE.get());

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.STONE), 50))
                .thenWaitUntil(() -> assertCount(helper, container(helper, chest).getItem(0), Items.STONE, 3))
                .thenIdle(40)
                .thenExecute(() -> assertCount(helper, container(helper, chest).getItem(0), Items.STONE, 3))
                .thenSucceed();
    }

    private static void pullerWaitsForSignal(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos puller = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        container(helper, chest).setItem(0, new ItemStack(Items.STONE, 2));
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.EAST));
        deviceEntity(helper, puller).changeSettings(TransferSettings.DEFAULT.withRedstone(RedstoneMode.HIGH_SIGNAL));

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 0,
                        "stone pulled without a signal"))
                .thenExecute(() -> place(helper, puller.above(), Blocks.REDSTONE_BLOCK.defaultBlockState()))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 2,
                        "stone pulled with a signal"))
                .thenSucceed();
    }

    private static void pusherBlacklist(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos pusher = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        place(helper, pusher, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, pusher).changeSettings(TransferSettings.DEFAULT.withFilter(
                FilterSlots.EMPTY.with(0, key(Items.STONE)).withMode(FilterMode.DENY)));

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.STONE), 5))
                .thenExecute(() -> assertInserted(helper, key(Items.DIRT), 2))
                .thenWaitUntil(() -> assertCount(helper, container(helper, chest).getItem(0), Items.DIRT, 2))
                .thenIdle(20)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 5,
                        "stone left in the network"))
                .thenSucceed();
    }

    private static void matchModeNormalizesItemKeys(final GameTestHelper helper) {
        final ItemKey pristine = key(Items.DIAMOND_PICKAXE);
        final ItemKey worn = wornPickaxe();

        helper.assertFalse(pristine.equals(worn), String.valueOf("a pristine and a worn pickaxe were equal"));
        helper.assertTrue(
                pristine.normalized(FilterMatchMode.IGNORE_DURABILITY)
                        .equals(worn.normalized(FilterMatchMode.IGNORE_DURABILITY)),
                String.valueOf("ignoring durability did not equate a pristine and a worn pickaxe"));
        helper.assertFalse(
                pristine.normalized(FilterMatchMode.EXACT).equals(worn.normalized(FilterMatchMode.EXACT)),
                String.valueOf("exact match mode equated a pristine and a worn pickaxe"));
        helper.succeed();
    }

    private static void pusherBlacklistIgnoresDurabilityWithMatchMode(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos pusher = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        place(helper, pusher, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, pusher).changeSettings(TransferSettings.DEFAULT
                .withFilter(FilterSlots.EMPTY.with(0, key(Items.DIAMOND_PICKAXE)).withMode(FilterMode.DENY))
                .withMatchMode(FilterMatchMode.IGNORE_DURABILITY));
        final ItemKey worn = wornPickaxe();

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, worn, 1))
                .thenExecute(() -> assertInserted(helper, key(Items.STONE), 5))
                .thenWaitUntil(() -> assertCount(helper, container(helper, chest).getItem(0), Items.STONE, 5))
                .thenIdle(20)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(worn), 1,
                        "the worn pickaxe left the network although its exact match is blacklisted"))
                .thenSucceed();
    }

    private static void nexusCountsRoles(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, CABLE.above(), device(NexusBlocks.PULLER.get(), Direction.EAST));
        place(helper, TARGET, device(NexusBlocks.PUSHER.get(), Direction.EAST));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    final NetworkStatistics statistics =
                            helper.<NexusBlockEntity>getBlockEntity(NEXUS).statistics();
                    helper.assertTrue(statistics.count(DeviceRole.PULLER) == 1
                                    && statistics.count(DeviceRole.PUSHER) == 1
                                    && statistics.count(DeviceRole.STORAGE) == 1,
                            String.valueOf("the Nexus counts " + statistics.devicesByRole()));
                })
                .thenSucceed();
    }

    private static void deviceArmsGlow(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos puller = CABLE.above();
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.EAST));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockProperty(puller, TransferDeviceBlock.POWERED, true))
                .thenSucceed();
    }

    private static void speedUpgradesMakePullerFaster(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        container(helper, chest).setItem(0, new ItemStack(Items.STONE, 30));
        final BlockPos puller = CABLE.above();
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.EAST));
        for (int i = 0; i < 4; i++) {
            install(helper, puller, NexusItems.SPEED_UPGRADE.get());
        }

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> helper.assertTrue(network(helper).amountOf(key(Items.STONE)) >= 18,
                        String.valueOf("in 60 ticks only " + network(helper).amountOf(key(Items.STONE))
                                + " stone, fewer than a Puller working every 3 ticks moves")))
                .thenSucceed();
    }

    private static void pullerPaysForWork(final GameTestHelper helper) {
        buildNetwork(helper);
        placePullerBeside(helper, Items.STONE, STONES_TO_MOVE);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(network(helper).amountOf(key(Items.STONE)) == STONES_TO_MOVE,
                        String.valueOf("the stone has not moved yet")))
                .thenIdle(PAST_THE_LAST_OPERATION_TICKS)
                .thenExecute(() -> helper.assertValueEqual(NETWORK_FILL - poolEnergy(helper),
                        STONES_TO_MOVE * OperationKind.TRANSFER.baseCost(), String.valueOf("FE paid")))
                .thenSucceed();
    }

    private static void idlePullerCostsNothing(final GameTestHelper helper) {
        buildNetwork(helper);
        placePullerBeside(helper, Items.STONE, 0);

        helper.startSequence()
                .thenIdle(PAST_THE_LAST_OPERATION_TICKS)
                .thenExecute(() -> helper.assertValueEqual(poolEnergy(helper), (long) NETWORK_FILL,
                        String.valueOf("FE left")))
                .thenSucceed();
    }

    private static void pullerNeedsEnergyToWork(final GameTestHelper helper) {
        buildNetwork(helper);
        final EnergyBuffer cell = helper.<EnergyCellBlockEntity>getBlockEntity(CELL).energyBuffer();
        while (cell.extract(Long.MAX_VALUE, Action.EXECUTE) > 0) {
            continue;
        }
        cell.insert(OperationKind.TRANSFER.baseCost() - 1, Action.EXECUTE);
        placePullerBeside(helper, Items.STONE, STONES_TO_MOVE);

        helper.startSequence()
                .thenIdle(PAST_THE_LAST_OPERATION_TICKS)
                .thenExecute(() -> helper.assertValueEqual(0L, network(helper).amountOf(key(Items.STONE)),
                        String.valueOf("stone moved without the FE to pay for it")))
                .thenSucceed();
    }

    private static void dearPriceIsPaid(final GameTestHelper helper) {
        buildNetwork(helper);
        final NexusBlockEntity nexus = helper.<NexusBlockEntity>getBlockEntity(NEXUS);
        final OperationUpgrades fastest = OperationUpgrades.speedOnly(OperationPrice.MAX_SPEED_UPGRADES);

        helper.startSequence()
                .thenIdle(PAST_THE_LAST_OPERATION_TICKS)
                .thenExecute(() -> {
                    final long price = OperationPrice.of(OperationKind.ASSEMBLER_RUN, nexus.statistics().devices(),
                            fastest);
                    helper.assertTrue(price > EnergyCellTier.BASIC.maxTransfer(),
                            String.valueOf("the price " + price + " is not above what a Basic cell gives"));
                    helper.assertTrue(OperationToll.affords(nexus, OperationKind.ASSEMBLER_RUN, fastest),
                            String.valueOf("a charged network cannot afford " + price));
                    final long before = poolEnergy(helper);
                    OperationToll.charge(nexus, OperationKind.ASSEMBLER_RUN, fastest,
                            nexus.component(NetworkComponentTypes.ENERGY_ACCOUNT).portableTerminals());
                    helper.assertValueEqual(before - poolEnergy(helper), price, String.valueOf("FE paid"));
                })
                .thenSucceed();
    }

    private static long poolEnergy(final GameTestHelper helper) {
        return helper.<NexusBlockEntity>getBlockEntity(NEXUS).energy().stored();
    }

    /**
     * A network that the test first {@code buildNetwork}s must exist before this is called.
     */
    private static void placePullerBeside(final GameTestHelper helper, final Item item, final int count) {
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        if (count > 0) {
            container(helper, chest).setItem(0, new ItemStack(item, count));
        }
        place(helper, CABLE.above(), device(NexusBlocks.PULLER.get(), Direction.EAST));
    }

    private static void stackUpgradeMovesStack(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        container(helper, chest).setItem(0, new ItemStack(Items.STONE, 64));
        final BlockPos puller = CABLE.above();
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.EAST));
        install(helper, puller, NexusItems.STACK_UPGRADE.get());

        helper.startSequence()
                .thenIdle(15)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 64,
                        "stone moved in the first operation"))
                .thenSucceed();
    }

    private static void pusherWithoutRegulator(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos pusher = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        place(helper, pusher, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, pusher).changeSettings(keepingStone(3));

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.STONE), 50))
                .thenWaitUntil(() -> helper.assertTrue(container(helper, chest).getItem(0).getCount() >= 6,
                        String.valueOf("the chest holds " + container(helper, chest).getItem(0)
                                + ", a Pusher without a Regulator Upgrade should not stop at 3")))
                .thenSucceed();
    }

    private static void pullerWithRegulatorLeavesStock(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        container(helper, chest).setItem(0, new ItemStack(Items.STONE, 10));
        final BlockPos puller = CABLE.above();
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.EAST));
        deviceEntity(helper, puller).changeSettings(keepingStone(4));
        install(helper, puller, NexusItems.REGULATOR_UPGRADE.get());

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 6,
                        "stone taken"))
                .thenIdle(40)
                .thenExecute(() -> assertCount(helper, container(helper, chest).getItem(0), Items.STONE, 4))
                .thenSucceed();
    }

    private static void filtersSurviveResourceSwitch(final GameTestHelper helper) {
        final TransferSettings items = TransferSettings.DEFAULT.withFilter(FilterSlots.EMPTY.with(0, key(Items.STONE)));

        final TransferSettings back = items.withResource(TransferResource.FLUID)
                .withResource(TransferResource.ENERGY).withResource(TransferResource.ITEM);

        helper.assertTrue(key(Items.STONE).equals(back.filter().resourceAt(0)),
                String.valueOf("the item filter lists " + back.filter().entries() + " after switching back"));
        helper.assertTrue(items.withResource(TransferResource.FLUID).filter().entries().isEmpty(),
                String.valueOf("the fluid filter took the item filter's entries"));
        helper.succeed();
    }

    private static void deviceTakesOnlyItsUpgrades(final GameTestHelper helper) {
        final UpgradeLimits limits = TransferKind.PULLER.upgradeLimits();
        final SimpleContainer upgrades = new SimpleContainer(TransferDeviceBlockEntity.UPGRADE_SLOTS);

        helper.assertFalse(limits.accepts(new ItemStack(NexusItems.FORTUNE_UPGRADE.get()), upgrades),
                String.valueOf("a Puller took a Fortune Upgrade, which only a Remover takes"));
        upgrades.setItem(0, new ItemStack(NexusItems.REGULATOR_UPGRADE.get()));
        helper.assertFalse(limits.accepts(new ItemStack(NexusItems.REGULATOR_UPGRADE.get()), upgrades),
                String.valueOf("a device took a second Regulator Upgrade"));
        helper.assertTrue(limits.accepts(new ItemStack(NexusItems.SPEED_UPGRADE.get()), upgrades),
                String.valueOf("a device refused a Speed Upgrade"));
        helper.succeed();
    }

    private static void speedUpgradesShareOneSlot(final GameTestHelper helper) {
        final UpgradeContainer upgrades = new UpgradeContainer(
                TransferDeviceBlockEntity.UPGRADE_SLOTS, TransferKind.PULLER.upgradeLimits(), () -> { });
        final ItemStack fourSpeeds = new ItemStack(NexusItems.SPEED_UPGRADE.get(), 4);

        helper.assertTrue(upgrades.canPlaceItem(0, fourSpeeds),
                String.valueOf("a device refused four Speed Upgrades in one slot"));
        helper.assertTrue(upgrades.capacityOf(0, fourSpeeds) == 4,
                String.valueOf("slot 0 fits only " + upgrades.capacityOf(0, fourSpeeds) + " Speed Upgrades"));

        upgrades.setItem(0, fourSpeeds);
        upgrades.setItem(1, new ItemStack(NexusItems.STACK_UPGRADE.get()));
        upgrades.setItem(2, new ItemStack(NexusItems.REGULATOR_UPGRADE.get()));
        upgrades.setItem(3, new ItemStack(NexusItems.CAPACITY_UPGRADE.get(), 3));

        helper.assertTrue(upgrades.count(UpgradeTypes.SPEED) == 4 && upgrades.count(UpgradeTypes.STACK) == 1
                        && upgrades.count(UpgradeTypes.REGULATOR) == 1 && upgrades.count(UpgradeTypes.CAPACITY) == 3,
                String.valueOf("four upgrade slots could not hold max Speed, Stack, Regulator and Capacity"
                        + " together"));
        helper.succeed();
    }

    private static void capacityUpgradesAddFilterSlots(final GameTestHelper helper) {
        final UpgradeContainer upgrades = new UpgradeContainer(
                TransferDeviceBlockEntity.UPGRADE_SLOTS, TransferKind.PULLER.upgradeLimits(), () -> { });

        helper.assertTrue(TransferDeviceBlockEntity.filterSlotCount(upgrades) == TransferDeviceBlockEntity.FILTER_SLOTS,
                String.valueOf("a device without Capacity Upgrades offered "
                        + TransferDeviceBlockEntity.filterSlotCount(upgrades) + " filter slots"));

        upgrades.setItem(0, new ItemStack(NexusItems.CAPACITY_UPGRADE.get(), 3));

        helper.assertTrue(TransferDeviceBlockEntity.filterSlotCount(upgrades) == 36,
                String.valueOf("three Capacity Upgrades gave " + TransferDeviceBlockEntity.filterSlotCount(upgrades)
                        + " filter slots, not 36"));
        helper.succeed();
    }

    private static void placerPlacesBlock(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.STONE), 3))
                .thenExecute(() -> placeDevice(helper, NexusBlocks.PLACER.get(),
                        TransferSettings.DEFAULT.withFilter(FilterSlots.EMPTY.with(0, key(Items.STONE)))))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.STONE, FRONT))
                .thenIdle(30)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 2,
                        "stone left in the network after one block"))
                .thenSucceed();
    }

    private static void placerDropsItems(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.STONE), 3))
                .thenExecute(() -> placeDevice(helper, NexusBlocks.PLACER.get(), TransferSettings.DEFAULT
                        .withFilter(FilterSlots.EMPTY.with(0, key(Items.STONE))).withWorldMode(WorldMode.ITEMS)))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 0,
                        "stone left in the network"))
                .thenExecute(() -> helper.assertItemEntityPresent(Items.STONE))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, FRONT))
                .thenSucceed();
    }

    private static void placerPoursFluid(final GameTestHelper helper) {
        buildNetwork(helper);
        addFluidCell(helper);
        final FluidKey water = new FluidKey(FluidResource.of(Fluids.WATER));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper,
                        network(helper).insert(water, 1000, Action.EXECUTE, Actor.NOBODY), 1000, "water accepted"))
                .thenExecute(() -> placeDevice(helper, NexusBlocks.PLACER.get(), TransferSettings.DEFAULT
                        .withResource(TransferResource.FLUID).withFilter(FilterSlots.EMPTY.with(0, water))))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.WATER, FRONT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(water), 0, "water left"))
                .thenSucceed();
    }

    private static void removerBreaksBlock(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, FRONT, Blocks.STONE.defaultBlockState());
        placeDevice(helper, NexusBlocks.REMOVER.get(), TransferSettings.DEFAULT);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, FRONT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.COBBLESTONE)), 1,
                        "cobblestone the stone dropped"))
                .thenSucceed();
    }

    private static void removerWithSilkTouch(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, FRONT, Blocks.STONE.defaultBlockState());
        placeDevice(helper, NexusBlocks.REMOVER.get(), TransferSettings.DEFAULT);
        install(helper, DEVICE, NexusItems.SILK_TOUCH_UPGRADE.get());

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, FRONT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 1,
                        "stone taken whole"))
                .thenSucceed();
    }

    private static void removerWhitelist(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, FRONT, Blocks.DIRT.defaultBlockState());
        placeDevice(helper, NexusBlocks.REMOVER.get(),
                TransferSettings.DEFAULT.withFilter(FilterSlots.EMPTY.with(0, key(Items.STONE))));

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertBlockPresent(Blocks.DIRT, FRONT))
                .thenExecute(() -> place(helper, FRONT, Blocks.STONE.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, FRONT))
                .thenSucceed();
    }

    private static void removerPicksUpItems(final GameTestHelper helper) {
        buildNetwork(helper);
        placeDevice(helper, NexusBlocks.REMOVER.get(), TransferSettings.DEFAULT.withWorldMode(WorldMode.ITEMS));
        helper.spawnItem(Items.DIAMOND, FRONT.getX() + 0.5F, FRONT.getY() + 0.2F, FRONT.getZ() + 0.5F);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.DIAMOND)), 1,
                        "diamonds picked up"))
                .thenExecute(() -> helper.assertItemEntityNotPresent(Items.DIAMOND))
                .thenSucceed();
    }

    private static void removerTakesFluid(final GameTestHelper helper) {
        buildNetwork(helper);
        addFluidCell(helper);
        place(helper, FRONT, Blocks.WATER.defaultBlockState());
        placeDevice(helper, NexusBlocks.REMOVER.get(), TransferSettings.DEFAULT.withResource(TransferResource.FLUID));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper,
                        network(helper).amountOf(new FluidKey(FluidResource.of(Fluids.WATER))), 1000, "water taken"))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, FRONT))
                .thenSucceed();
    }

    private static void pullerWithTag(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos puller = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        container(helper, chest).setItem(0, new ItemStack(Items.STONE, 2));
        container(helper, chest).setItem(1, new ItemStack(Items.BIRCH_LOG, 3));
        place(helper, puller, device(NexusBlocks.PULLER.get(), Direction.EAST));
        deviceEntity(helper, puller).changeSettings(TransferSettings.DEFAULT.withFilter(oakLogsByTag()));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.BIRCH_LOG)), 3,
                        "birch logs pulled by the tag of oak logs"))
                .thenIdle(20)
                .thenExecute(() -> assertCount(helper, container(helper, chest).getItem(0), Items.STONE, 2))
                .thenSucceed();
    }

    private static void pusherWithTag(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos pusher = CABLE.above();
        final BlockPos chest = TARGET.above();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        place(helper, pusher, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, pusher).changeSettings(TransferSettings.DEFAULT.withFilter(oakLogsByTag()));

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, key(Items.STONE), 5))
                .thenExecute(() -> assertInserted(helper, key(Items.BIRCH_LOG), 2))
                .thenWaitUntil(() -> assertCount(helper, container(helper, chest).getItem(0), Items.BIRCH_LOG, 2))
                .thenIdle(20)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 5,
                        "stone left in the network"))
                .thenSucceed();
    }

    private static void filterTagSurvivesSaving(final GameTestHelper helper) {
        final RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        final FilterSlots filter = oakLogsByTag().with(1, key(Items.STONE));
        final Tag saved = FilterSlots.CODEC.encodeStart(ops, filter).getOrThrow();

        final FilterSlots loaded = FilterSlots.CODEC.parse(ops, saved).getOrThrow();

        helper.assertTrue(loaded.equals(filter), String.valueOf("filter read back as " + loaded));
        helper.succeed();
    }

    /**
     * A whitelist of oak logs, listed by the tag {@code minecraft:logs}.
     */
    private static FilterSlots oakLogsByTag() {
        return new FilterSlots(FilterMode.ALLOW,
                List.of(new FilterSlots.Entry(0, key(Items.OAK_LOG), ResourceLocation.withDefaultNamespace("logs"))));
    }

    private static void placeDevice(final GameTestHelper helper, final TransferDeviceBlock block,
                                    final TransferSettings settings) {
        place(helper, DEVICE, device(block, Direction.EAST));
        deviceEntity(helper, DEVICE).changeSettings(settings);
    }

    private static void addFluidCell(final GameTestHelper helper) {
        helper.<StorageVaultBlockEntity>getBlockEntity(VAULT).cells()
                .setItem(1, new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.FLUID).get(CellTier.ONE_K).get()));
    }

    private static TransferSettings keepingStone(final long amount) {
        return TransferSettings.DEFAULT
                .withFilter(FilterSlots.EMPTY.with(0, key(Items.STONE)))
                .withDelivery(DeliveryMode.KEEP_STOCKED)
                .withKeepAmount(0, amount);
    }

    /**
     * Puts {@code upgrade} into the first free upgrade slot of the device at {@code pos}.
     */
    private static void install(final GameTestHelper helper, final BlockPos pos, final Item upgrade) {
        final Container upgrades = deviceEntity(helper, pos).upgrades();
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            if (upgrades.getItem(slot).isEmpty()) {
                upgrades.setItem(slot, new ItemStack(upgrade));
                return;
            }
        }
        throw new GameTestAssertException("no free upgrade slot");
    }

    private static List<TransferDeviceBlock> allDevices() {
        return List.of(NexusBlocks.PULLER.get(), NexusBlocks.PUSHER.get(), NexusBlocks.PLACER.get(),
                NexusBlocks.REMOVER.get());
    }

    private static void devicesTakeCablesOnEverySideButTheirFace(final GameTestHelper helper) {
        final BlockPos centre = new BlockPos(5, 3, 5);
        for (Direction side : Direction.values()) {
            place(helper, centre.relative(side), cable());
        }
        for (TransferDeviceBlock block : allDevices()) {
            for (Direction facing : Direction.values()) {
                assertAttachedExceptFace(helper, centre, block, facing);
            }
        }
        helper.succeed();
    }

    private static void assertAttachedExceptFace(
            final GameTestHelper helper, final BlockPos pos, final TransferDeviceBlock block, final Direction facing) {
        place(helper, pos, device(block, facing));
        for (Direction side : Direction.values()) {
            final boolean attached = SideConnections.isAttached(helper.getBlockState(pos), side);
            helper.assertTrue(attached == (side != facing), String.valueOf(block.kind() + " facing " + facing
                    + (attached ? " has an arm on " : " lacks an arm on ") + side));
        }
    }

    private static void devicesShowNetworkColor(final GameTestHelper helper) {
        buildNetwork(helper);
        final Map<BlockPos, BlockState> devices = Map.of(
                CABLE.above(), device(NexusBlocks.PULLER.get(), Direction.EAST),
                CABLE.north(), device(NexusBlocks.PUSHER.get(), Direction.NORTH),
                CABLE.south(), device(NexusBlocks.PLACER.get(), Direction.SOUTH),
                TARGET, device(NexusBlocks.REMOVER.get(), Direction.EAST));
        devices.forEach((pos, state) -> place(helper, pos, state));

        helper.startSequence()
                .thenExecute(() -> helper.<NexusBlockEntity>getBlockEntity(NEXUS)
                        .recolor(NetworkColoring.colorOf(DyeColor.RED)))
                .thenWaitUntil(() -> devices.keySet().forEach(pos -> helper.assertBlockProperty(
                        pos, NetworkDeviceBlock.NETWORK_COLOR, DyeColor.RED)))
                .thenSucceed();
    }

    private static void assemblerTakesCablesOnEverySideButItsFace(final GameTestHelper helper) {
        final BlockPos centre = new BlockPos(5, 3, 5);
        for (Direction side : Direction.values()) {
            place(helper, centre.relative(side), cable());
        }
        for (Direction facing : Direction.values()) {
            place(helper, centre,
                    NexusBlocks.ASSEMBLER.get().defaultBlockState().setValue(AssemblerBlock.FACING, facing));
            for (Direction side : Direction.values()) {
                final boolean attached = SideConnections.isAttached(helper.getBlockState(centre), side);
                helper.assertTrue(attached == (side != facing), String.valueOf("Assembler facing " + facing
                        + (attached ? " has a port on " : " lacks a port on ") + side));
            }
        }
        helper.succeed();
    }

    private static void assemblerShowsNetworkColor(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos assembler = CABLE.above();
        place(helper, assembler, NexusBlocks.ASSEMBLER.get().defaultBlockState()
                .setValue(AssemblerBlock.FACING, Direction.EAST));

        helper.startSequence()
                .thenExecute(() -> helper.<NexusBlockEntity>getBlockEntity(NEXUS)
                        .recolor(NetworkColoring.colorOf(DyeColor.GREEN)))
                .thenWaitUntil(() -> helper.assertBlockProperty(
                        assembler, NetworkDeviceBlock.NETWORK_COLOR, DyeColor.GREEN))
                .thenSucceed();
    }

    private static void generatorTakesCablesOnEverySideButItsFront(final GameTestHelper helper) {
        final BlockPos centre = new BlockPos(5, 3, 5);
        for (Direction side : Direction.values()) {
            place(helper, centre.relative(side), cable());
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            place(helper, centre,
                    NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get().defaultBlockState()
                            .setValue(GeneratorBlock.FACING, facing));
            for (Direction side : Direction.values()) {
                final boolean attached = SideConnections.isAttached(helper.getBlockState(centre), side);
                helper.assertTrue(attached == (side != facing), String.valueOf("Coal Generator facing " + facing
                        + (attached ? " has a port on " : " lacks a port on ") + side));
            }
        }
        helper.succeed();
    }

    private static void energyCellShowsItsChargeLevel(final GameTestHelper helper) {
        final BlockPos cell = new BlockPos(5, 3, 5);
        place(helper, cell, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        helper.assertBlockProperty(cell, EnergyCellBlock.CHARGE, 0);
        for (int step = 0; step < SIXTY_PERCENT_STEPS; step++) {
            TestEnergy.charge(helper, cell, (int) EnergyCellTier.BASIC.maxTransfer());
        }

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockProperty(cell, EnergyCellBlock.CHARGE, 5))
                .thenSucceed();
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        TestEnergy.fill(helper, CELL, NETWORK_FILL);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        helper.<StorageVaultBlockEntity>getBlockEntity(VAULT).cells()
                .setItem(0, new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get()));
        place(helper, CABLE, cable());
    }

    private static void assertInserted(final GameTestHelper helper, final ItemKey resource, final long amount) {
        assertAmount(helper, network(helper).insert(resource, amount, Action.EXECUTE, Actor.NOBODY), amount,
                "accepted " + resource.name().getString());
    }

    private static void assertCount(final GameTestHelper helper, final ItemStack stack, final Item item,
                                    final int expected) {
        helper.assertTrue(stack.is(item) && stack.getCount() == expected,
                String.valueOf("found " + stack + ", expected " + expected + " " + item));
    }

    private static void assertAmount(final GameTestHelper helper, final long actual, final long expected,
                                     final String what) {
        helper.assertTrue(actual == expected, String.valueOf(what + ": " + actual + ", expected " + expected));
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.<NexusBlockEntity>getBlockEntity(NEXUS)
                .component(NetworkComponentTypes.STORAGE).storage();
    }

    private static TransferDeviceBlockEntity deviceEntity(final GameTestHelper helper, final BlockPos pos) {
        return helper.<TransferDeviceBlockEntity>getBlockEntity(pos);
    }

    private static Container container(final GameTestHelper helper, final BlockPos pos) {
        return helper.<BaseContainerBlockEntity>getBlockEntity(pos);
    }

    private static ItemKey key(final Item item) {
        return ItemKey.of(new ItemStack(item));
    }

    private static ItemKey wornPickaxe() {
        final ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.setDamageValue(5);
        return ItemKey.of(stack);
    }

    private static BlockState cable() {
        return NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState();
    }

    private static BlockState device(final TransferDeviceBlock block, final Direction facing) {
        return block.defaultBlockState().setValue(TransferDeviceBlock.FACING, facing);
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
