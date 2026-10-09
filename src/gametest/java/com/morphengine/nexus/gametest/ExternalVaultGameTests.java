package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.ExternalVaultBlock;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.external.ExternalVaultSettings;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.ExternalAccess;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The External Vault in a world: a chest it touches is part of the network's storage. Every test starts from the same
 * network along the north edge: a Nexus, a charged Energy Cell south of it, a Storage Vault with a cell east of it, a
 * cable east of the vault and an External Vault east of the cable, facing a chest.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ExternalVaultGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos EXTERNAL = CABLE.east();
    private static final BlockPos CHEST = EXTERNAL.east();
    private static final int STONE_IN_CHEST = 20;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("external_vault_lists_what_its_chest_holds", ExternalVaultGameTests::listsChest),
            Map.entry("external_vault_notices_a_change_made_to_the_chest", ExternalVaultGameTests::noticesChange),
            Map.entry("network_takes_items_out_of_the_chest", ExternalVaultGameTests::takesOut),
            Map.entry("network_stores_into_the_chest_when_it_ranks_first", ExternalVaultGameTests::storesIntoChest),
            Map.entry("network_stores_elsewhere_when_the_chest_ranks_last", ExternalVaultGameTests::storesElsewhere),
            Map.entry("a_read_only_vault_takes_nothing_in", ExternalVaultGameTests::readOnlyTakesNothingIn),
            Map.entry("a_whitelist_hides_what_it_does_not_list", ExternalVaultGameTests::whitelistHides),
            Map.entry("a_removed_vault_takes_its_chest_out_of_the_network",
                    ExternalVaultGameTests::removalLeavesNetwork),
            Map.entry("a_vault_against_a_block_of_a_network_uses_nothing",
                    ExternalVaultGameTests::refusesNetworkBlock),
            Map.entry("external_vaults_count_as_storages", ExternalVaultGameTests::countsAsStorage),
            Map.entry("external_vault_settings_survive_saving", ExternalVaultGameTests::settingsSurviveSaving),
            Map.entry("external_vault_takes_only_its_upgrades", ExternalVaultGameTests::takesOnlyItsUpgrades),
            Map.entry("capacity_upgrades_widen_the_filter_of_a_vault", ExternalVaultGameTests::capacityWidensFilter),
            Map.entry("a_device_moves_a_limited_amount_through_a_vault_in_a_tick",
                    ExternalVaultGameTests::limitsDevices),
            Map.entry("speed_upgrades_let_a_vault_move_more_in_a_tick", ExternalVaultGameTests::speedMovesMore));

    private ExternalVaultGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(ExternalVaultGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void listsChest(final GameTestHelper helper) {
        buildNetworkWithChest(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenSucceed();
    }

    private static void noticesChange(final GameTestHelper helper) {
        buildNetworkWithChest(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> container(helper).setItem(1, new ItemStack(Items.DIRT, 7)))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(dirt()), 7,
                        "dirt that appeared in the chest"))
                .thenExecute(() -> container(helper).setItem(0, ItemStack.EMPTY))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), 0,
                        "stone taken out of the chest by hand"))
                .thenSucceed();
    }

    private static void takesOut(final GameTestHelper helper) {
        buildNetworkWithChest(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper,
                        network(helper).extract(stone(), 8, Action.EXECUTE, Actor.NOBODY), 8, "stone taken"))
                .thenExecute(() -> {
                    assertAmount(helper, container(helper).getItem(0).getCount(), STONE_IN_CHEST - 8,
                            "stone left in the chest");
                    assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST - 8,
                            "stone left in the network");
                })
                .thenSucceed();
    }

    private static void storesIntoChest(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        external(helper).setPriority(10);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper,
                        network(helper).insert(dirt(), 8, Action.EXECUTE, Actor.NOBODY), 8, "dirt stored"))
                .thenExecute(() -> {
                    assertAmount(helper, countIn(container(helper), Items.DIRT), 8, "dirt in the chest");
                    assertAmount(helper, network(helper).amountOf(dirt()), 8, "dirt in the network");
                })
                .thenSucceed();
    }

    private static void storesElsewhere(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        external(helper).setPriority(-10);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper,
                        network(helper).insert(dirt(), 30, Action.EXECUTE, Actor.NOBODY), 30, "dirt stored"))
                .thenExecute(() -> assertAmount(helper, countIn(container(helper), Items.DIRT), 0,
                        "dirt in the chest, which ranks last"))
                .thenSucceed();
    }

    private static void readOnlyTakesNothingIn(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        external(helper).setPriority(10);
        external(helper).changeSettings(ExternalVaultSettings.DEFAULT.withAccess(ExternalAccess.READ_ONLY));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> network(helper).insert(dirt(), 30, Action.EXECUTE, Actor.NOBODY))
                .thenExecute(() -> {
                    assertAmount(helper, countIn(container(helper), Items.DIRT), 0, "dirt in a read only chest");
                    assertAmount(helper, network(helper).extract(stone(), 5, Action.EXECUTE, Actor.NOBODY), 5,
                            "stone taken from a read only chest");
                })
                .thenSucceed();
    }

    private static void whitelistHides(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        container(helper).setItem(1, new ItemStack(Items.DIRT, 7));
        external(helper).changeSettings(ExternalVaultSettings.DEFAULT.withFilter(
                FilterSlots.EMPTY.with(0, stone())));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(dirt()), 0,
                        "dirt that is not listed"))
                .thenExecute(() -> external(helper).changeSettings(external(helper).settings().withFilter(
                        FilterSlots.EMPTY.withMode(FilterMode.DENY).with(0, stone()))))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(dirt()), 7,
                        "dirt after the filter turned to a blacklist"))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), 0,
                        "stone that is blacklisted"))
                .thenSucceed();
    }

    private static void removalLeavesNetwork(final GameTestHelper helper) {
        buildNetworkWithChest(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> helper.setBlock(EXTERNAL, Blocks.AIR.defaultBlockState()))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), 0,
                        "stone left in the network"))
                .thenSucceed();
    }

    private static void refusesNetworkBlock(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, CHEST, NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get().defaultBlockState());
        helper.<GeneratorBlockEntity>getBlockEntity(CHEST).input().setItem(0, new ItemStack(Items.COAL, 5));
        place(helper, EXTERNAL, externalVault(Direction.EAST));

        helper.startSequence()
                .thenIdle(50)
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(ItemKey.of(new ItemStack(Items.COAL))),
                        0, "coal of a generator that is part of the network"))
                .thenSucceed();
    }

    private static void countsAsStorage(final GameTestHelper helper) {
        buildNetworkWithChest(helper);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        helper.<NexusBlockEntity>getBlockEntity(NEXUS).statistics()
                                .count(DeviceRole.STORAGE) == 2,
                        String.valueOf("the Nexus does not count the External Vault as a storage")))
                .thenSucceed();
    }

    private static void settingsSurviveSaving(final GameTestHelper helper) {
        final RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        final ExternalVaultSettings settings = ExternalVaultSettings.DEFAULT
                .withFilter(FilterSlots.EMPTY.with(0, stone())).withAccess(ExternalAccess.READ_ONLY);
        final Tag saved = ExternalVaultSettings.CODEC.encodeStart(ops, settings).getOrThrow();

        final ExternalVaultSettings loaded = ExternalVaultSettings.CODEC.parse(ops, saved).getOrThrow();

        helper.assertTrue(loaded.equals(settings), String.valueOf("settings read back as " + loaded));
        helper.succeed();
    }

    private static void buildNetworkWithChest(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, CHEST, Blocks.CHEST.defaultBlockState());
        container(helper).setItem(0, new ItemStack(Items.STONE, STONE_IN_CHEST));
        place(helper, EXTERNAL, externalVault(Direction.EAST));
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        TestEnergy.charge(helper, CELL, 10_000);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        helper.<StorageVaultBlockEntity>getBlockEntity(VAULT).cells()
                .setItem(0, new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get()));
        place(helper, CABLE, NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState());
    }

    private static BlockState externalVault(final Direction facing) {
        return NexusBlocks.EXTERNAL_VAULT.get().defaultBlockState().setValue(ExternalVaultBlock.FACING, facing);
    }

    private static ExternalVaultBlockEntity external(final GameTestHelper helper) {
        return helper.<ExternalVaultBlockEntity>getBlockEntity(EXTERNAL);
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.<NexusBlockEntity>getBlockEntity(NEXUS)
                .component(NetworkComponentTypes.STORAGE).storage();
    }

    private static Container container(final GameTestHelper helper) {
        return helper.<BaseContainerBlockEntity>getBlockEntity(CHEST);
    }

    private static int countIn(final Container container, final Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) {
                count += container.getItem(slot).getCount();
            }
        }
        return count;
    }

    private static ItemKey stone() {
        return ItemKey.of(new ItemStack(Items.STONE));
    }

    private static ItemKey dirt() {
        return ItemKey.of(new ItemStack(Items.DIRT));
    }

    private static void assertAmount(
            final GameTestHelper helper, final long actual, final long expected, final String what) {
        helper.assertTrue(actual == expected, String.valueOf(what + ": " + actual + ", expected " + expected));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }

    private static void takesOnlyItsUpgrades(final GameTestHelper helper) {
        final SimpleContainer upgrades = new SimpleContainer(ExternalVaultBlockEntity.UPGRADE_SLOTS);
        final UpgradeLimits limits = ExternalVaultBlockEntity.UPGRADE_LIMITS;

        helper.assertTrue(limits.accepts(new ItemStack(NexusItems.CHUNK_LOADER_UPGRADE.get()), upgrades),
                String.valueOf("a vault refused a Chunk Loader Upgrade"));
        helper.assertTrue(limits.accepts(new ItemStack(NexusItems.CAPACITY_UPGRADE.get()), upgrades),
                String.valueOf("a vault refused a Capacity Upgrade"));
        helper.assertFalse(limits.accepts(new ItemStack(NexusItems.FORTUNE_UPGRADE.get()), upgrades),
                String.valueOf("a vault took a Fortune Upgrade"));
        helper.succeed();
    }

    private static void capacityWidensFilter(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        container(helper).setItem(1, new ItemStack(Items.DIRT, 7));
        external(helper).changeSettings(ExternalVaultSettings.DEFAULT.withFilter(
                FilterSlots.EMPTY.with(0, stone()).with(ExternalVaultBlockEntity.FILTER_SLOTS + 1, dirt())));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(dirt()), 0,
                        "dirt listed in a slot that is not there yet"))
                .thenExecute(() -> external(helper).upgrades()
                        .setItem(0, new ItemStack(NexusItems.CAPACITY_UPGRADE.get())))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(dirt()), 7,
                        "dirt once the slot is there"))
                .thenSucceed();
    }

    private static void limitsDevices(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        external(helper).setPriority(10);

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper,
                        network(helper).insert(dirt(), 100, Action.EXECUTE, Actor.NOBODY), 100, "dirt stored"))
                .thenExecute(() -> assertAmount(helper, countIn(container(helper), Items.DIRT),
                        ExternalVaultBlockEntity.BASE_STEPS_PER_TICK, "dirt a device put in the chest in one tick"))
                .thenSucceed();
    }

    private static void speedMovesMore(final GameTestHelper helper) {
        buildNetworkWithChest(helper);
        external(helper).setPriority(10);
        external(helper).upgrades().setItem(0, new ItemStack(NexusItems.SPEED_UPGRADE.get(),
                ExternalVaultBlockEntity.MAX_SPEED_UPGRADES));

        helper.startSequence()
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(stone()), STONE_IN_CHEST,
                        "stone in the network"))
                .thenExecute(() -> assertAmount(helper,
                        network(helper).insert(dirt(), 100, Action.EXECUTE, Actor.NOBODY), 100, "dirt stored"))
                .thenExecute(() -> assertAmount(helper, countIn(container(helper), Items.DIRT),
                        ExternalVaultBlockEntity.BASE_STEPS_PER_TICK
                                + ExternalVaultBlockEntity.STEPS_PER_SPEED_UPGRADE
                                * ExternalVaultBlockEntity.MAX_SPEED_UPGRADES,
                        "dirt a device put in the chest in one tick with Speed Upgrades"))
                .thenSucceed();
    }
}
