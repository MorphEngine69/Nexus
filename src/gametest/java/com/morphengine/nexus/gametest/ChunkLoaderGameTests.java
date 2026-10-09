package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.NetworkReceiverBlockEntity;
import com.morphengine.nexus.block.entity.NetworkTransmitterBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.NexusLinkBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 100;
    private static final int TEST_SIZE = 8;
    private static final int TEST_HEIGHT = 4;
    private static final BlockPos FIRST = new BlockPos(2, 1, 2);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "chunk_loader_upgrade_holds_the_chunk_while_it_is_in_a_device", ChunkLoaderGameTests::holdsTheChunk);

    private ChunkLoaderGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(ChunkLoaderGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void holdsTheChunk(final GameTestHelper helper) {
        holdsTheChunkWhileInADevice(helper);
        holdsTheChunkForTransmitterAndReceiver(helper);
        holdsTheChunkForNexusVaultAndCell(helper);
        takesOneChunkLoaderOnly(helper);
        helper.succeed();
    }

    private static void holdsTheChunkWhileInADevice(final GameTestHelper helper) {
        final BlockPos second = sameChunkNeighbour(helper, FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("the chunk was held before any upgrade"));

        placePuller(helper, FIRST);
        puller(helper, FIRST).upgrades().setItem(0, new ItemStack(NexusItems.SPEED_UPGRADE.get()));
        helper.assertFalse(held(helper, FIRST), String.valueOf("a Speed Upgrade held the chunk"));

        puller(helper, FIRST).upgrades().setItem(1, loader());
        helper.assertTrue(held(helper, FIRST), String.valueOf("the chunk was not held with an upgrade in"));

        place(helper, second, NexusBlocks.NEXUS_LINK.get().defaultBlockState());
        link(helper, second).upgrades().setItem(0, loader());
        puller(helper, FIRST).upgrades().setItem(1, ItemStack.EMPTY);
        helper.assertTrue(held(helper, FIRST),
                String.valueOf("the chunk was let go while another device in it held an upgrade"));

        link(helper, second).upgrades().setItem(0, ItemStack.EMPTY);
        helper.assertFalse(held(helper, FIRST), String.valueOf("the chunk was held with every upgrade out"));

        puller(helper, FIRST).upgrades().setItem(1, loader());
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("the chunk was held after its device broke"));
        helper.destroyBlock(second);
    }

    private static void holdsTheChunkForTransmitterAndReceiver(final GameTestHelper helper) {
        place(helper, FIRST, NexusBlocks.NETWORK_TRANSMITTER.get().defaultBlockState());
        transmitter(helper, FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), String.valueOf("a transmitter did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("a broken transmitter held the chunk"));

        place(helper, FIRST, NexusBlocks.NETWORK_RECEIVER.get().defaultBlockState());
        receiver(helper, FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), String.valueOf("a receiver did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("a broken receiver held the chunk"));
    }

    private static void holdsTheChunkForNexusVaultAndCell(final GameTestHelper helper) {
        place(helper, FIRST, NexusBlocks.NEXUS.get().defaultBlockState());
        helper.<NexusBlockEntity>getBlockEntity(FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), String.valueOf("a Nexus did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("a broken Nexus held the chunk"));

        place(helper, FIRST, NexusBlocks.STORAGE_VAULT.get().defaultBlockState());
        helper.<StorageVaultBlockEntity>getBlockEntity(FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), String.valueOf("a Storage Vault did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("a broken Storage Vault held the chunk"));

        place(helper, FIRST, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        helper.<EnergyCellBlockEntity>getBlockEntity(FIRST).upgrades().setItem(0, loader());
        helper.assertTrue(held(helper, FIRST), String.valueOf("an Energy Cell did not hold the chunk"));
        helper.destroyBlock(FIRST);
        helper.assertFalse(held(helper, FIRST), String.valueOf("a broken Energy Cell held the chunk"));
    }

    private static void takesOneChunkLoaderOnly(final GameTestHelper helper) {
        place(helper, FIRST, NexusBlocks.NEXUS_LINK.get().defaultBlockState());
        final Container upgrades = link(helper, FIRST).upgrades();
        upgrades.setItem(0, loader());

        helper.assertFalse(upgrades.canPlaceItem(1, loader()),
                String.valueOf("a device took a second Chunk Loader Upgrade"));
        helper.destroyBlock(FIRST);
    }

    /**
     * @return whether the chunk of {@code pos} is held by the ticket of a block entity
     */
    private static boolean held(final GameTestHelper helper, final BlockPos pos) {
        final ForcedChunksSavedData tickets = helper.getLevel().getDataStorage()
                .computeIfAbsent(ForcedChunksSavedData.factory(), ForcedChunksSavedData.FILE_ID);
        final long chunk = new ChunkPos(helper.absolutePos(pos)).toLong();
        final Set<Integer> owners = new HashSet<>();
        final ResourceLocation controller = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "chunk_loader");
        BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(TEST_SIZE, TEST_HEIGHT, TEST_SIZE))
                .forEach(inTest -> owners.add(Objects.hash(controller, helper.absolutePos(inTest))));
        return ticketsOf(tickets).entrySet().stream()
                .anyMatch(ticket -> owners.contains(ticket.getKey().hashCode()) && ticket.getValue().contains(chunk));
    }

    /**
     * Reads the tickets by reflection: the type that names their owners is not public.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, LongSet> ticketsOf(final ForcedChunksSavedData tickets) {
        try {
            final Object tracker = tickets.getBlockForcedChunks();
            final Field chunks = tracker.getClass().getDeclaredField("chunks");
            chunks.setAccessible(true);
            return (Map<Object, LongSet>) chunks.get(tracker);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read the chunk tickets", e);
        }
    }

    /**
     * @return a position beside {@code pos} in the same chunk
     */
    private static BlockPos sameChunkNeighbour(final GameTestHelper helper, final BlockPos pos) {
        final BlockPos east = pos.east();
        return new ChunkPos(helper.absolutePos(east)).equals(new ChunkPos(helper.absolutePos(pos))) ? east : pos.west();
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
        return helper.<TransferDeviceBlockEntity>getBlockEntity(pos);
    }

    private static NexusLinkBlockEntity link(final GameTestHelper helper, final BlockPos pos) {
        return helper.<NexusLinkBlockEntity>getBlockEntity(pos);
    }

    private static NetworkTransmitterBlockEntity transmitter(final GameTestHelper helper, final BlockPos pos) {
        return helper.<NetworkTransmitterBlockEntity>getBlockEntity(pos);
    }

    private static NetworkReceiverBlockEntity receiver(final GameTestHelper helper, final BlockPos pos) {
        return helper.<NetworkReceiverBlockEntity>getBlockEntity(pos);
    }
}
