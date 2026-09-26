package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.NetworkStatistics;
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
import com.morphengine.nexus.transfer.TransferSettings;
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
            Map.entry("device_arms_glow_with_energy", TransferGameTests::deviceArmsGlow));

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
        deviceEntity(helper, pusher).changeSettings(TransferSettings.DEFAULT
                .withFilter(FilterSlots.EMPTY.with(0, key(Items.STONE)))
                .withDelivery(DeliveryMode.KEEP_STOCKED)
                .withKeepAmount(0, 3));

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
