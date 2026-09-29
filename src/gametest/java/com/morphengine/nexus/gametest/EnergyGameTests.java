package com.morphengine.nexus.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.CellSpec;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.EnergyKey;
import com.morphengine.nexus.transfer.TransferResource;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
 * FE going in and out of a network: Pullers and Pushers move it between the
 * network and the block their face touches, a Nexus takes it from other mods
 * and passes it on to its network, and Energy Vault Cells join the pool.
 *
 * <p>Every test starts from the same network along the north edge: a Nexus, an
 * Energy Cell charged with {@value #CHARGE} FE south of it, a Storage Vault
 * east of it and a cable east of the vault.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class EnergyGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    /** What one insert into a Basic Energy Cell takes at most. */
    private static final int CHARGE = 1000;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos DEVICE = CABLE.above();
    private static final BlockPos TARGET = DEVICE.east();
    private static final TransferSettings ENERGY = TransferSettings.DEFAULT.withResource(TransferResource.ENERGY);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("pusher_charges_block_beside_it", EnergyGameTests::pusherChargesBlockBesideIt),
            Map.entry("puller_draws_energy_into_network", EnergyGameTests::pullerDrawsEnergyIntoNetwork),
            Map.entry("pusher_feeds_another_network_through_its_nexus", EnergyGameTests::pusherFeedsAnotherNexus),
            Map.entry("pusher_keeps_feeding_another_network", EnergyGameTests::pusherKeepsFeedingAnotherNexus),
            Map.entry("nexus_fills_energy_vault_cell_after_energy_cells", EnergyGameTests::nexusFillsVaultCell),
            Map.entry("aborted_nexus_insert_changes_nothing", EnergyGameTests::abortedNexusInsert),
            Map.entry("puller_set_to_items_leaves_energy", EnergyGameTests::pullerSetToItemsLeavesEnergy),
            Map.entry("device_saved_without_resource_takes_its_filters", EnergyGameTests::savedSettingsInferResource));

    private EnergyGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "energy"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void pusherChargesBlockBesideIt(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, TARGET, cell());
        place(helper, DEVICE, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, DEVICE).changeSettings(ENERGY);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, stored(helper, TARGET), CHARGE, "FE in the cell beside"))
                .thenExecute(() -> assertAmount(helper, nexus(helper, NEXUS).energy().stored(), 0,
                        "FE left in the network"))
                .thenSucceed();
    }

    private static void pullerDrawsEnergyIntoNetwork(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, TARGET, cell());
        charge(helper, TARGET);
        place(helper, DEVICE, device(NexusBlocks.PULLER.get(), Direction.EAST));
        deviceEntity(helper, DEVICE).changeSettings(ENERGY);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, stored(helper, TARGET), 0, "FE left in the cell beside"))
                .thenWaitUntil(() -> assertAmount(helper, resourcesEnergy(helper), 2L * CHARGE,
                        "FE the network lists"))
                .thenSucceed();
    }

    private static void pusherFeedsAnotherNexus(final GameTestHelper helper) {
        buildNetwork(helper);
        final BlockPos otherCell = TARGET.above();
        place(helper, TARGET, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, otherCell, cell());
        place(helper, DEVICE, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, DEVICE).changeSettings(ENERGY);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, stored(helper, otherCell), CHARGE,
                        "FE in the other network's cell"))
                .thenWaitUntil(() -> assertAmount(helper, nexus(helper, TARGET).energy().stored(), CHARGE,
                        "FE in the other network"))
                .thenSucceed();
    }

    private static void pusherKeepsFeedingAnotherNexus(final GameTestHelper helper) {
        buildNetwork(helper);
        final EnergyCellBlockEntity source = helper.getBlockEntity(CELL, EnergyCellBlockEntity.class);
        for (int i = 0; i < 30; i++) {
            source.energyBuffer().insert(CHARGE, Action.EXECUTE);
        }
        final BlockPos otherCell = TARGET.above();
        place(helper, TARGET, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, otherCell, cell());
        place(helper, DEVICE, device(NexusBlocks.PUSHER.get(), Direction.EAST));
        deviceEntity(helper, DEVICE).changeSettings(ENERGY);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stored(helper, otherCell) >= 12 * CHARGE,
                        Component.literal("the other network's cell holds only " + stored(helper, otherCell)
                                + " FE, the source still " + stored(helper, CELL))))
                .thenSucceed();
    }

    private static void nexusFillsVaultCell(final GameTestHelper helper) {
        buildNetwork(helper);
        vault(helper).cells().setItem(0, energyVaultCell());

        helper.startSequence()
                .thenWaitUntil(() -> assertVaultCellInPool(helper))
                .thenExecute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        final int accepted = nexusHandler(helper).insert(5 * CHARGE, transaction);
                        assertAmount(helper, accepted, 5L * CHARGE, "FE the Nexus accepted");
                        transaction.commit();
                    }
                })
                .thenExecute(() -> assertAmount(helper, stored(helper, CELL), 2L * CHARGE, "FE in the Energy Cell"))
                .thenExecute(() -> assertAmount(helper, storageEnergy(helper), 4L * CHARGE,
                        "FE in the Energy Vault Cell"))
                .thenExecute(() -> assertAmount(helper, resourcesEnergy(helper), 6L * CHARGE,
                        "FE the network lists"))
                .thenSucceed();
    }

    private static void abortedNexusInsert(final GameTestHelper helper) {
        buildNetwork(helper);
        vault(helper).cells().setItem(0, energyVaultCell());

        helper.startSequence()
                .thenWaitUntil(() -> assertVaultCellInPool(helper))
                .thenExecute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        nexusHandler(helper).insert(5 * CHARGE, transaction);
                    }
                })
                .thenExecute(() -> assertAmount(helper, stored(helper, CELL), CHARGE, "FE in the Energy Cell"))
                .thenExecute(() -> assertAmount(helper, storageEnergy(helper), 0, "FE in the Energy Vault Cell"))
                .thenSucceed();
    }

    private static void pullerSetToItemsLeavesEnergy(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, TARGET, cell());
        charge(helper, TARGET);
        place(helper, DEVICE, device(NexusBlocks.PULLER.get(), Direction.EAST));

        helper.startSequence()
                .thenIdle(60)
                .thenExecute(() -> assertAmount(helper, stored(helper, TARGET), CHARGE, "FE left in the cell beside"))
                .thenSucceed();
    }

    private static void savedSettingsInferResource(final GameTestHelper helper) {
        final String saved = """
                {"filter": {"whitelist": true, "entries": [{"slot": 0, "resource": {"type": "nexus:energy"}}]}}
                """;
        final TransferSettings settings = TransferSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(saved))
                .getOrThrow();
        helper.assertTrue(settings.resource() == TransferResource.ENERGY,
                Component.literal("settings saved with FE listed read as " + settings.resource()));
        helper.assertTrue(TransferSettings.DEFAULT.resource() == TransferResource.ITEM,
                Component.literal("a new device moves " + TransferSettings.DEFAULT.resource()));
        helper.succeed();
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, cell());
        charge(helper, CELL);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        place(helper, CABLE, NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState());
    }

    private static void assertVaultCellInPool(final GameTestHelper helper) {
        final CellSpec spec = CellTier.ONE_K.specFor(CellKind.ENERGY);
        assertAmount(helper, nexus(helper, NEXUS).energy().capacity(),
                EnergyCellTier.BASIC.capacity() + spec.totalBytes() * spec.unitsPerByte(), "FE the pool holds");
    }

    private static void charge(final GameTestHelper helper, final BlockPos pos) {
        try (Transaction transaction = Transaction.openRoot()) {
            final int accepted = handler(helper, pos).insert(CHARGE, transaction);
            assertAmount(helper, accepted, CHARGE, "FE charged into " + pos);
            transaction.commit();
        }
    }

    private static long stored(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, EnergyCellBlockEntity.class).energyBuffer().stored();
    }

    private static long resourcesEnergy(final GameTestHelper helper) {
        return nexus(helper, NEXUS).resources().amountOf(EnergyKey.INSTANCE);
    }

    private static long storageEnergy(final GameTestHelper helper) {
        return nexus(helper, NEXUS).component(NetworkComponentTypes.STORAGE).storage().amountOf(EnergyKey.INSTANCE);
    }

    private static EnergyHandler nexusHandler(final GameTestHelper helper) {
        return handler(helper, NEXUS);
    }

    private static EnergyHandler handler(final GameTestHelper helper, final BlockPos pos) {
        final EnergyHandler handler = helper.getLevel()
                .getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.WEST);
        if (handler == null) {
            throw helper.assertionException(pos, Component.literal("no energy handler"));
        }
        return handler;
    }

    private static void assertAmount(final GameTestHelper helper, final long actual, final long expected,
                                     final String what) {
        helper.assertTrue(actual == expected, Component.literal(what + ": " + actual + ", expected " + expected));
    }

    private static NexusBlockEntity nexus(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, NexusBlockEntity.class);
    }

    private static StorageVaultBlockEntity vault(final GameTestHelper helper) {
        return helper.getBlockEntity(VAULT, StorageVaultBlockEntity.class);
    }

    private static TransferDeviceBlockEntity deviceEntity(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, TransferDeviceBlockEntity.class);
    }

    private static ItemStack energyVaultCell() {
        return new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ENERGY).get(CellTier.ONE_K).get());
    }

    private static BlockState cell() {
        return NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState();
    }

    private static BlockState device(final TransferDeviceBlock block, final Direction facing) {
        return block.defaultBlockState().setValue(TransferDeviceBlock.FACING, facing);
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
