package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.WirelessAccessComponent;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
 * A network beyond its cables: a Network Transmitter carries it to the Network
 * Receiver its card is linked to, and a Nexus Link lets Nexus Terminals reach
 * it within its range.
 *
 * <p>Every test starts from the same network along the north edge: a Nexus, a
 * charged Energy Cell south of it, a Storage Vault with a cell east of it and a
 * cable east of the vault. In the far corner stands a receiver with a Puller
 * north of it, facing a chest.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class WirelessGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos TRANSMITTER = CABLE.east();
    private static final BlockPos LINK = CABLE.south();
    private static final BlockPos RECEIVER = new BlockPos(7, 1, 7);
    private static final BlockPos PULLER = RECEIVER.north();
    private static final BlockPos CHEST = PULLER.north();

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("transmitter_carries_network_to_receiver", WirelessGameTests::transmitterCarriesNetwork),
            Map.entry("taking_the_card_out_cuts_the_far_side", WirelessGameTests::takingCardOutCutsFarSide),
            Map.entry("nexus_link_reaches_within_its_range", WirelessGameTests::linkReachesWithinRange),
            Map.entry("range_upgrade_extends_the_link", WirelessGameTests::rangeUpgradeExtendsLink));

    private WirelessGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "wireless"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void transmitterCarriesNetwork(final GameTestHelper helper) {
        buildBothSides(helper);
        container(helper).setItem(0, new ItemStack(Items.STONE, 3));
        insertCard(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), 3,
                        "stone pulled in on the receiver's side"))
                .thenSucceed();
    }

    private static void takingCardOutCutsFarSide(final GameTestHelper helper) {
        buildBothSides(helper);
        container(helper).setItem(0, new ItemStack(Items.STONE, 1));
        insertCard(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), 1, "stone pulled in"))
                .thenExecute(() -> transmitter(helper).card().setItem(0, ItemStack.EMPTY))
                .thenIdle(5)
                .thenExecute(() -> container(helper).setItem(0, new ItemStack(Items.STONE, 1)))
                .thenIdle(40)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), 1,
                        "stone in the network once the card is out"))
                .thenSucceed();
    }

    private static void linkReachesWithinRange(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, LINK, NexusBlocks.NEXUS_LINK.get().defaultBlockState());
        final Vec3 link = Vec3.atCenterOf(helper.absolutePos(LINK));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(access(helper).hasAccessPoints(),
                        Component.literal("the network has no access point")))
                .thenExecute(() -> helper.assertTrue(reaches(helper, link.add(NexusLinkBlockEntity.BASE_RANGE - 1, 0,
                        0)), Component.literal("the link does not reach within its range")))
                .thenExecute(() -> helper.assertFalse(reaches(helper, link.add(NexusLinkBlockEntity.BASE_RANGE + 1,
                        0, 0)), Component.literal("the link reaches beyond its range")))
                .thenSucceed();
    }

    private static void rangeUpgradeExtendsLink(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, LINK, NexusBlocks.NEXUS_LINK.get().defaultBlockState());
        helper.getBlockEntity(LINK, NexusLinkBlockEntity.class).upgrades()
                .setItem(0, new ItemStack(NexusItems.RANGE_UPGRADE.get()));
        final Vec3 far = Vec3.atCenterOf(helper.absolutePos(LINK)).add(NexusLinkBlockEntity.BASE_RANGE + 1, 0, 0);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(reaches(helper, far),
                        Component.literal("a Range Upgrade did not extend the link")))
                .thenSucceed();
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        charge(helper);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        helper.getBlockEntity(VAULT, StorageVaultBlockEntity.class).cells()
                .setItem(0, new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get()));
        place(helper, CABLE, NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState());
    }

    private static void buildBothSides(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, TRANSMITTER, NexusBlocks.NETWORK_TRANSMITTER.get().defaultBlockState());
        place(helper, RECEIVER, NexusBlocks.NETWORK_RECEIVER.get().defaultBlockState());
        place(helper, CHEST, Blocks.CHEST.defaultBlockState());
        place(helper, PULLER, NexusBlocks.PULLER.get().defaultBlockState()
                .setValue(TransferDeviceBlock.FACING, Direction.NORTH));
    }

    private static void insertCard(final GameTestHelper helper) {
        final ItemStack card = new ItemStack(NexusItems.NETWORK_CARD.get());
        card.set(NexusDataComponents.LINKED_RECEIVER.get(),
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(RECEIVER)));
        transmitter(helper).card().setItem(0, card);
    }

    private static void charge(final GameTestHelper helper) {
        final EnergyHandler handler = helper.getLevel()
                .getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(CELL), Direction.WEST);
        helper.assertTrue(handler != null, Component.literal("no energy cell"));
        try (Transaction transaction = Transaction.openRoot()) {
            handler.insert(10_000, transaction);
            transaction.commit();
        }
    }

    private static boolean reaches(final GameTestHelper helper, final Vec3 at) {
        return access(helper).reaches(helper.getLevel().dimension(), at);
    }

    private static WirelessAccessComponent access(final GameTestHelper helper) {
        return helper.getBlockEntity(NEXUS, NexusBlockEntity.class).component(NetworkComponentTypes.WIRELESS_ACCESS);
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.getBlockEntity(NEXUS, NexusBlockEntity.class).component(NetworkComponentTypes.STORAGE).storage();
    }

    private static NetworkTransmitterBlockEntity transmitter(final GameTestHelper helper) {
        return helper.getBlockEntity(TRANSMITTER, NetworkTransmitterBlockEntity.class);
    }

    private static Container container(final GameTestHelper helper) {
        return helper.getBlockEntity(CHEST, BaseContainerBlockEntity.class);
    }

    private static ItemKey stone() {
        return ItemKey.of(new ItemStack(Items.STONE));
    }

    private static void assertAmount(final GameTestHelper helper, final long actual, final long expected,
                                     final String what) {
        helper.assertTrue(actual == expected, Component.literal(what + ": " + actual + ", expected " + expected));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
