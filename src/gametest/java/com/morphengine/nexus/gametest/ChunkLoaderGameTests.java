package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.NetworkReceiverBlockEntity;
import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A Chunk Loader Upgrade keeps the chunk of its device loaded for as long as
 * it sits in the device: not once the upgrade is taken out or the device is
 * broken, and not while another device in the chunk still holds one.
 *
 * <p>All in one test in an environment of its own: the tickets are per chunk, so
 * another test's device in the same chunk would show in these checks.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ChunkLoaderGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 100;
    private static final BlockPos FIRST = new BlockPos(2, 1, 2);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "chunk_loader_upgrade_holds_the_chunk_while_it_is_in_a_device", ChunkLoaderGameTests::holdsTheChunk);

    private ChunkLoaderGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "chunk_loader"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void holdsTheChunk(final GameTestHelper helper) {
        holdsTheChunkWhileInADevice(helper);
        holdsTheChunkForTransmitterAndReceiver(helper);
        takesOneChunkLoaderOnly(helper);
        helper.succeed();
    }

    private static void holdsTheChunkWhileInADevice(final GameTestHelper helper) {
        final BlockPos second = sameChunkNeighbour(helper, FIRST);
        helper.assertFalse(held(helper, FIRST), Component.literal("the chunk was held before any upgrade"));

        placePuller(helper, FIRST);
        puller(helper, FIRST).upgrades().setItem(0, new ItemStack(NexusItems.SPEED_UPGRADE.get()));
        helper.assertFalse(held(helper, FIRST), Component.literal("a Speed Upgrade held the chunk"));

        puller(helper, FIRST).upgrades().setItem(1, loader());
        helper.assertTrue(held(helper, FIRST), Component.literal("the chunk was not held with an upgrade in"));

        place(helper, second, NexusBlocks.NEXUS_LINK.get().defaultBlockState());
        link(helper, second).upgrades().setItem(0, loader());
        puller(helper, FIRST).upgrades().setItem(1, ItemStack.EMPTY);
        helper.assertTrue(held(helper, FIRST),
                Component.literal("the chunk was let go while another device in it held an upgrade"));

        link(helper, second).upgrades().setItem(0, ItemStack.EMPTY);
        helper.assertFalse(held(helper, FIRST), Component.literal("the chunk was held with every upgrade out"));

        puller(helper, FIRST).upgrades().setItem(1, loader());
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), Component.literal("the chunk was held after its device broke"));
        helper.destroyBlock(second);
    }

    private static void holdsTheChunkForTransmitterAndReceiver(final GameTestHelper helper) {
        place(helper, FIRST, NexusBlocks.NETWORK_TRANSMITTER.get().defaultBlockState());
        transmitter(helper, FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), Component.literal("a transmitter did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), Component.literal("a broken transmitter held the chunk"));

        place(helper, FIRST, NexusBlocks.NETWORK_RECEIVER.get().defaultBlockState());
        receiver(helper, FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), Component.literal("a receiver did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), Component.literal("a broken receiver held the chunk"));
    }

    private static void takesOneChunkLoaderOnly(final GameTestHelper helper) {
        place(helper, FIRST, NexusBlocks.NEXUS_LINK.get().defaultBlockState());
        final Container upgrades = link(helper, FIRST).upgrades();
        upgrades.setItem(0, loader());

        helper.assertFalse(upgrades.canPlaceItem(1, loader()),
                Component.literal("a device took a second Chunk Loader Upgrade"));
        helper.destroyBlock(FIRST);
    }

    /**
     * @return whether the chunk of {@code pos} is held by the ticket of a block entity
     */
    private static boolean held(final GameTestHelper helper, final BlockPos pos) {
        final TicketStorage tickets = helper.getLevel().getDataStorage().computeIfAbsent(TicketStorage.TYPE);
        return tickets.getTickets(ChunkPos.pack(helper.absolutePos(pos))).stream()
                .anyMatch(ticket -> ticket.getType() == NeoForgeMod.BLOCK_TICKET.value());
    }

    /**
     * @return a position beside {@code pos} in the same chunk
     */
    private static BlockPos sameChunkNeighbour(final GameTestHelper helper, final BlockPos pos) {
        final BlockPos east = pos.east();
        return ChunkPos.pack(helper.absolutePos(east)) == ChunkPos.pack(helper.absolutePos(pos)) ? east : pos.west();
    }

    private static ItemStack loader() {
        return new ItemStack(NexusItems.CHUNK_LOADER_UPGRADE.get());
    }

    private static void placePuller(final GameTestHelper helper, final BlockPos pos) {
        place(helper, pos, NexusBlocks.PULLER.get().defaultBlockState()
                .setValue(TransferDeviceBlock.FACING, Direction.EAST));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }

    private static TransferDeviceBlockEntity puller(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, TransferDeviceBlockEntity.class);
    }

    private static NexusLinkBlockEntity link(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, NexusLinkBlockEntity.class);
    }

    private static NetworkTransmitterBlockEntity transmitter(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, NetworkTransmitterBlockEntity.class);
    }

    private static NetworkReceiverBlockEntity receiver(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, NetworkReceiverBlockEntity.class);
    }
}
