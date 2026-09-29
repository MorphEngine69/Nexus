package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
import com.morphengine.nexus.api.resource.FilterMatchMode;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.transport.RedstoneMode;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.transfer.DeliveryMode;
import com.morphengine.nexus.transfer.TransferResource;
import com.morphengine.nexus.transfer.TransferSettings;
import com.morphengine.nexus.upgrade.UpgradeContainer;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Pullers and Pushers in a world: they work with the block their face touches
 * as that block offers itself on that face, keep amounts stocked and follow
 * their redstone mode.
 *
 * <p>Every test starts from the same network along the north edge: a Nexus, a
 * charged Energy Cell south of it, a Storage Vault with a cell east of it and a
 * cable east of the vault.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class TransferGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos TARGET = CABLE.east();

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
            Map.entry("stack_upgrade_moves_a_stack_at_once", TransferGameTests::stackUpgradeMovesStack),
            Map.entry("pusher_without_regulator_ignores_amount", TransferGameTests::pusherWithoutRegulator),
            Map.entry("puller_with_regulator_leaves_stock", TransferGameTests::pullerWithRegulatorLeavesStock),
            Map.entry("filters_survive_switching_resource", TransferGameTests::filtersSurviveResourceSwitch),
            Map.entry("device_takes_only_its_upgrades", TransferGameTests::deviceTakesOnlyItsUpgrades),
            Map.entry("speed_upgrades_share_one_slot", TransferGameTests::speedUpgradesShareOneSlot),
            Map.entry("capacity_upgrades_add_filter_slots", TransferGameTests::capacityUpgradesAddFilterSlots),
            Map.entry("match_mode_normalizes_item_keys", TransferGameTests::matchModeNormalizesItemKeys),
            Map.entry("pusher_blacklist_ignores_durability_with_match_mode",
                    TransferGameTests::pusherBlacklistIgnoresDurabilityWithMatchMode));

    private TransferGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "transfer"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
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
                        Component.literal("the chest still holds stone")))
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
                        Component.literal("the furnace took ore through its side")))
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

        helper.assertFalse(pristine.equals(worn), Component.literal("a pristine and a worn pickaxe were equal"));
        helper.assertTrue(
                pristine.normalized(FilterMatchMode.IGNORE_DURABILITY)
                        .equals(worn.normalized(FilterMatchMode.IGNORE_DURABILITY)),
                Component.literal("ignoring durability did not equate a pristine and a worn pickaxe"));
        helper.assertFalse(
                pristine.normalized(FilterMatchMode.EXACT).equals(worn.normalized(FilterMatchMode.EXACT)),
                Component.literal("exact match mode equated a pristine and a worn pickaxe"));
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
                            helper.getBlockEntity(NEXUS, NexusBlockEntity.class).statistics();
                    helper.assertTrue(statistics.count(DeviceRole.PULLER) == 1
                                    && statistics.count(DeviceRole.PUSHER) == 1
                                    && statistics.count(DeviceRole.STORAGE) == 1,
                            Component.literal("the Nexus counts " + statistics.devicesByRole()));
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
                .thenExecute(() -> helper.assertTrue(network(helper).amountOf(key(Items.STONE)) >= 20,
                        Component.literal("in 60 ticks only " + network(helper).amountOf(key(Items.STONE))
                                + " stone, fewer than a Puller working every 2 ticks moves")))
                .thenSucceed();
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
                        Component.literal("the chest holds " + container(helper, chest).getItem(0)
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
                Component.literal("the item filter lists " + back.filter().entries() + " after switching back"));
        helper.assertTrue(items.withResource(TransferResource.FLUID).filter().entries().isEmpty(),
                Component.literal("the fluid filter took the item filter's entries"));
        helper.succeed();
    }

    private static void deviceTakesOnlyItsUpgrades(final GameTestHelper helper) {
        final UpgradeLimits limits = TransferDeviceBlockEntity.UPGRADE_LIMITS;
        final SimpleContainer upgrades = new SimpleContainer(TransferDeviceBlockEntity.UPGRADE_SLOTS);

        helper.assertFalse(limits.accepts(new ItemStack(NexusItems.RANGE_UPGRADE.get()), upgrades),
                Component.literal("a device took an upgrade still in development"));
        upgrades.setItem(0, new ItemStack(NexusItems.REGULATOR_UPGRADE.get()));
        helper.assertFalse(limits.accepts(new ItemStack(NexusItems.REGULATOR_UPGRADE.get()), upgrades),
                Component.literal("a device took a second Regulator Upgrade"));
        helper.assertTrue(limits.accepts(new ItemStack(NexusItems.SPEED_UPGRADE.get()), upgrades),
                Component.literal("a device refused a Speed Upgrade"));
        helper.succeed();
    }

    private static void speedUpgradesShareOneSlot(final GameTestHelper helper) {
        final UpgradeContainer upgrades = new UpgradeContainer(
                TransferDeviceBlockEntity.UPGRADE_SLOTS, TransferDeviceBlockEntity.UPGRADE_LIMITS, () -> { });
        final ItemStack fourSpeeds = new ItemStack(NexusItems.SPEED_UPGRADE.get(), 4);

        helper.assertTrue(upgrades.canPlaceItem(0, fourSpeeds),
                Component.literal("a device refused four Speed Upgrades in one slot"));
        helper.assertTrue(upgrades.capacityOf(0, fourSpeeds) == 4,
                Component.literal("slot 0 fits only " + upgrades.capacityOf(0, fourSpeeds) + " Speed Upgrades"));

        upgrades.setItem(0, fourSpeeds);
        upgrades.setItem(1, new ItemStack(NexusItems.STACK_UPGRADE.get()));
        upgrades.setItem(2, new ItemStack(NexusItems.REGULATOR_UPGRADE.get()));
        upgrades.setItem(3, new ItemStack(NexusItems.CAPACITY_UPGRADE.get(), 3));

        helper.assertTrue(upgrades.count(UpgradeTypes.SPEED) == 4 && upgrades.count(UpgradeTypes.STACK) == 1
                        && upgrades.count(UpgradeTypes.REGULATOR) == 1 && upgrades.count(UpgradeTypes.CAPACITY) == 3,
                Component.literal("four upgrade slots could not hold max Speed, Stack, Regulator and Capacity"
                        + " together"));
        helper.succeed();
    }

    private static void capacityUpgradesAddFilterSlots(final GameTestHelper helper) {
        final UpgradeContainer upgrades = new UpgradeContainer(
                TransferDeviceBlockEntity.UPGRADE_SLOTS, TransferDeviceBlockEntity.UPGRADE_LIMITS, () -> { });

        helper.assertTrue(TransferDeviceBlockEntity.filterSlotCount(upgrades) == TransferDeviceBlockEntity.FILTER_SLOTS,
                Component.literal("a device without Capacity Upgrades offered "
                        + TransferDeviceBlockEntity.filterSlotCount(upgrades) + " filter slots"));

        upgrades.setItem(0, new ItemStack(NexusItems.CAPACITY_UPGRADE.get(), 3));

        helper.assertTrue(TransferDeviceBlockEntity.filterSlotCount(upgrades) == 36,
                Component.literal("three Capacity Upgrades gave " + TransferDeviceBlockEntity.filterSlotCount(upgrades)
                        + " filter slots, not 36"));
        helper.succeed();
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
        throw helper.assertionException(pos, Component.literal("no free upgrade slot"));
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        charge(helper, CELL);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        helper.getBlockEntity(VAULT, StorageVaultBlockEntity.class).cells()
                .setItem(0, new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get()));
        place(helper, CABLE, cable());
    }

    private static void charge(final GameTestHelper helper, final BlockPos pos) {
        final EnergyHandler handler = helper.getLevel()
                .getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.WEST);
        helper.assertTrue(handler != null, Component.literal("no energy cell at " + pos));
        try (Transaction transaction = Transaction.openRoot()) {
            handler.insert(10_000, transaction);
            transaction.commit();
        }
    }

    private static void assertInserted(final GameTestHelper helper, final ItemKey resource, final long amount) {
        assertAmount(helper, network(helper).insert(resource, amount, Action.EXECUTE, Actor.NOBODY), amount,
                "accepted " + resource.name().getString());
    }

    private static void assertCount(final GameTestHelper helper, final ItemStack stack, final Item item,
                                    final int expected) {
        helper.assertTrue(stack.is(item) && stack.getCount() == expected,
                Component.literal("found " + stack + ", expected " + expected + " " + item));
    }

    private static void assertAmount(final GameTestHelper helper, final long actual, final long expected,
                                     final String what) {
        helper.assertTrue(actual == expected, Component.literal(what + ": " + actual + ", expected " + expected));
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.getBlockEntity(NEXUS, NexusBlockEntity.class)
                .component(NetworkComponentTypes.STORAGE).storage();
    }

    private static TransferDeviceBlockEntity deviceEntity(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, TransferDeviceBlockEntity.class);
    }

    private static Container container(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
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
