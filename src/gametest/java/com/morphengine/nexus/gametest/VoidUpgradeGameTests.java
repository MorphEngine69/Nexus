package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.ExternalVaultBlock;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.entity.DeviceUpgrades;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.VoidUpgradeItem;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * The Void Upgrade in a world: what it lists is destroyed on its way into the network, and nothing else is. Every test
 * starts from a Nexus with a charged Energy Cell south of it and a Storage Vault with an item cell east of it.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class VoidUpgradeGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos EXTERNAL = CABLE.east();
    private static final BlockPos CHEST = EXTERNAL.east();
    private static final int AMOUNT = 10;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "a_vault_with_a_void_upgrade_destroys_what_the_upgrade_lists", VoidUpgradeGameTests::destroysListed,
            "a_void_upgrade_leaves_what_it_does_not_list_alone", VoidUpgradeGameTests::leavesOthers,
            "a_void_upgrade_does_not_touch_what_the_network_already_holds", VoidUpgradeGameTests::leavesHeld,
            "a_void_upgrade_with_an_empty_list_destroys_nothing", VoidUpgradeGameTests::emptyListDestroysNothing,
            "taking_the_void_upgrade_out_stops_the_destroying", VoidUpgradeGameTests::removalStops,
            "an_external_vault_with_a_void_upgrade_destroys_too", VoidUpgradeGameTests::externalVaultDestroys,
            "only_vaults_take_a_void_upgrade", VoidUpgradeGameTests::onlyVaultsTakeIt);

    private VoidUpgradeGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(VoidUpgradeGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void destroysListed(final GameTestHelper helper) {
        buildNetwork(helper);
        vault(helper).upgrades().setItem(0, voidUpgradeListing(stone()));

        helper.startSequence()
                .thenWaitUntil(() -> assertNetworkHasStorage(helper, 1))
                .thenExecute(() -> assertAmount(helper, insert(helper, stone(), AMOUNT), AMOUNT, "stone taken in"))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), 0, "stone kept"))
                .thenSucceed();
    }

    private static void leavesOthers(final GameTestHelper helper) {
        buildNetwork(helper);
        vault(helper).upgrades().setItem(0, voidUpgradeListing(stone()));

        helper.startSequence()
                .thenWaitUntil(() -> assertNetworkHasStorage(helper, 1))
                .thenExecute(() -> insert(helper, dirt(), AMOUNT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(dirt()), AMOUNT, "dirt kept"))
                .thenSucceed();
    }

    private static void leavesHeld(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenWaitUntil(() -> assertNetworkHasStorage(helper, 1))
                .thenExecute(() -> insert(helper, stone(), AMOUNT))
                .thenExecute(() -> vault(helper).upgrades().setItem(0, voidUpgradeListing(stone())))
                .thenExecute(() -> insert(helper, stone(), AMOUNT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), AMOUNT,
                        "stone held from before the upgrade"))
                .thenSucceed();
    }

    private static void emptyListDestroysNothing(final GameTestHelper helper) {
        buildNetwork(helper);
        vault(helper).upgrades().setItem(0, new ItemStack(NexusItems.VOID_UPGRADE.get()));

        helper.startSequence()
                .thenWaitUntil(() -> assertNetworkHasStorage(helper, 1))
                .thenExecute(() -> insert(helper, stone(), AMOUNT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), AMOUNT, "stone kept"))
                .thenSucceed();
    }

    private static void removalStops(final GameTestHelper helper) {
        buildNetwork(helper);
        vault(helper).upgrades().setItem(0, voidUpgradeListing(stone()));

        helper.startSequence()
                .thenWaitUntil(() -> assertNetworkHasStorage(helper, 1))
                .thenExecute(() -> vault(helper).upgrades().setItem(0, ItemStack.EMPTY))
                .thenExecute(() -> insert(helper, stone(), AMOUNT))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), AMOUNT,
                        "stone kept once the upgrade is gone"))
                .thenSucceed();
    }

    private static void externalVaultDestroys(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, CHEST, Blocks.CHEST.defaultBlockState());
        place(helper, EXTERNAL, NexusBlocks.EXTERNAL_VAULT.get().defaultBlockState()
                .setValue(ExternalVaultBlock.FACING, Direction.EAST));
        helper.<ExternalVaultBlockEntity>getBlockEntity(EXTERNAL).upgrades()
                .setItem(0, voidUpgradeListing(stone()));

        helper.startSequence()
                .thenWaitUntil(() -> assertNetworkHasStorage(helper, 2))
                .thenExecute(() -> assertAmount(helper, insert(helper, stone(), AMOUNT), AMOUNT, "stone taken in"))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), 0, "stone kept"))
                .thenSucceed();
    }

    private static void onlyVaultsTakeIt(final GameTestHelper helper) {
        final ItemStack upgrade = new ItemStack(NexusItems.VOID_UPGRADE.get());
        final SimpleContainer slots = new SimpleContainer(DeviceUpgrades.SIZE);

        helper.assertTrue(StorageVaultBlockEntity.UPGRADE_LIMITS.accepts(upgrade, slots),
                String.valueOf("a Storage Vault refused a Void Upgrade"));
        helper.assertTrue(ExternalVaultBlockEntity.UPGRADE_LIMITS.accepts(upgrade, slots),
                String.valueOf("an External Vault refused a Void Upgrade"));
        helper.assertFalse(DeviceUpgrades.LIMITS.accepts(upgrade, slots),
                String.valueOf("a Nexus or an Energy Cell took a Void Upgrade"));
        helper.succeed();
    }

    private static ItemStack voidUpgradeListing(final ItemKey resource) {
        final ItemStack stack = new ItemStack(NexusItems.VOID_UPGRADE.get());
        VoidUpgradeItem.setList(stack, FilterSlots.EMPTY.with(0, resource));
        return stack;
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        TestEnergy.charge(helper, CELL, 10_000);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        vault(helper).cells()
                .setItem(0, new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get()));
        place(helper, CABLE, NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState());
    }

    private static void assertNetworkHasStorage(final GameTestHelper helper, final int sources) {
        helper.assertTrue(network(helper).sourceCount() >= sources,
                String.valueOf("the network has " + network(helper).sourceCount() + " storages, expected "
                        + sources));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }

    private static StorageVaultBlockEntity vault(final GameTestHelper helper) {
        return helper.<StorageVaultBlockEntity>getBlockEntity(VAULT);
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.<NexusBlockEntity>getBlockEntity(NEXUS)
                .component(NetworkComponentTypes.STORAGE).storage();
    }

    private static long insert(final GameTestHelper helper, final ItemKey resource, final long amount) {
        return network(helper).insert(resource, amount, Action.EXECUTE, Actor.NOBODY);
    }

    private static void assertAmount(
            final GameTestHelper helper, final long actual, final long expected, final String what) {
        helper.assertTrue(actual == expected, String.valueOf(what + ": " + actual + ", expected " + expected));
    }

    private static ItemKey stone() {
        return ItemKey.of(new ItemStack(Items.STONE));
    }

    private static ItemKey dirt() {
        return ItemKey.of(new ItemStack(Items.DIRT));
    }
}
