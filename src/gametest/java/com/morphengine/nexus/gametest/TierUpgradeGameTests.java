package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.block.EnergyCellTier;
import com.morphengine.nexus.block.entity.EnergyCellBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.security.EditResult;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.SecurityEdit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tier upgrades in a world: a cell goes up one tier at a time, in order, and keeps all that it holds.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class TierUpgradeGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE = NEXUS.east();
    private static final BlockPos CELL = new BlockPos(5, 1, 6);
    private static final UUID OWNER = new UUID(0, 1);
    private static final long STORED = 5_000;
    private static final int PRIORITY = 17;
    private static final String NAME = "Spare power";

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("tier_upgrade_raises_a_cell_and_keeps_all_it_holds", TierUpgradeGameTests::keepsEverything),
            Map.entry("tier_upgrades_take_a_cell_to_the_top_in_order", TierUpgradeGameTests::goesUpInOrder),
            Map.entry("tier_upgrade_cannot_skip_a_tier", TierUpgradeGameTests::cannotSkip),
            Map.entry("tier_upgrade_does_not_lower_or_repeat_a_tier", TierUpgradeGameTests::doesNotRepeat),
            Map.entry("tier_upgrade_needs_sneaking", TierUpgradeGameTests::needsSneaking),
            Map.entry("tier_upgrade_is_not_used_up_in_creative", TierUpgradeGameTests::notUsedUpInCreative),
            Map.entry("tier_upgrade_makes_the_network_hold_more", TierUpgradeGameTests::networkHoldsMore),
            Map.entry("stranger_cannot_upgrade_a_cell_of_a_network", TierUpgradeGameTests::strangerCannotUpgrade));

    private TierUpgradeGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(TierUpgradeGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void keepsEverything(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.SURVIVAL);
        final EnergyCellBlockEntity cell = filledBasicCell(helper);
        final ItemStack chunkLoader = new ItemStack(NexusItems.CHUNK_LOADER_UPGRADE.get());
        cell.upgrades().setItem(0, chunkLoader.copy());

        final ItemStack upgrade = holding(player, NexusItems.ADVANCED_TIER_UPGRADE.get());
        final InteractionResult result = use(helper, player, upgrade, true);

        helper.assertTrue(result.consumesAction(), String.valueOf("the upgrade was not used"));
        helper.assertValueEqual(helper.getBlockState(CELL).getBlock(), NexusBlocks.ADVANCED_ENERGY_CELL.get(),
                String.valueOf("the block after the upgrade"));
        final EnergyCellBlockEntity upgraded = helper.<EnergyCellBlockEntity>getBlockEntity(CELL);
        helper.assertValueEqual(upgraded.energyBuffer().stored(), STORED, String.valueOf("FE kept"));
        helper.assertValueEqual(upgraded.energyBuffer().capacity(), EnergyCellTier.ADVANCED.capacity(),
                String.valueOf("capacity of the new tier"));
        helper.assertValueEqual(upgraded.energyPriority(), PRIORITY, String.valueOf("priority kept"));
        helper.assertValueEqual(upgraded.getDisplayName().getString(), NAME, String.valueOf("name kept"));
        helper.assertTrue(ItemStack.isSameItemSameComponents(upgraded.upgrades().getItem(0), chunkLoader),
                String.valueOf("the upgrade of the cell was lost"));
        helper.assertTrue(upgrade.isEmpty(), String.valueOf("the tier upgrade was not used up"));
        helper.assertTrue(itemsOnTheGround(helper).isEmpty(), String.valueOf("something fell to the ground"));
        leave(helper, player);
        helper.succeed();
    }

    private static void goesUpInOrder(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.SURVIVAL);
        filledBasicCell(helper);

        for (Item tierUpgrade : new Item[] {NexusItems.ADVANCED_TIER_UPGRADE.get(),
                NexusItems.SUPERIOR_TIER_UPGRADE.get(), NexusItems.QUANTUM_TIER_UPGRADE.get()}) {
            use(helper, player, holding(player, tierUpgrade), true);
        }

        helper.assertValueEqual(helper.getBlockState(CELL).getBlock(), NexusBlocks.QUANTUM_ENERGY_CELL.get(),
                String.valueOf("the block at the top of the line"));
        helper.assertValueEqual(helper.<EnergyCellBlockEntity>getBlockEntity(CELL).energyBuffer().stored(),
                STORED, String.valueOf("FE kept through three upgrades"));
        leave(helper, player);
        helper.succeed();
    }

    private static void cannotSkip(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.SURVIVAL);
        filledBasicCell(helper);
        final ItemStack upgrade = holding(player, NexusItems.SUPERIOR_TIER_UPGRADE.get());

        final InteractionResult result = use(helper, player, upgrade, true);

        helper.assertFalse(result.consumesAction(), String.valueOf("a tier was skipped"));
        helper.assertValueEqual(helper.getBlockState(CELL).getBlock(), NexusBlocks.BASIC_ENERGY_CELL.get(),
                String.valueOf("the block after a refused upgrade"));
        helper.assertValueEqual(upgrade.getCount(), 1, String.valueOf("a refused upgrade is not used up"));
        leave(helper, player);
        helper.succeed();
    }

    private static void doesNotRepeat(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.SURVIVAL);
        place(helper, CELL, NexusBlocks.ADVANCED_ENERGY_CELL.get().defaultBlockState());
        final ItemStack upgrade = holding(player, NexusItems.ADVANCED_TIER_UPGRADE.get());

        final InteractionResult result = use(helper, player, upgrade, true);

        helper.assertFalse(result.consumesAction(), String.valueOf("a tier was repeated"));
        helper.assertValueEqual(helper.getBlockState(CELL).getBlock(), NexusBlocks.ADVANCED_ENERGY_CELL.get(),
                String.valueOf("the block after a refused upgrade"));
        helper.assertValueEqual(upgrade.getCount(), 1, String.valueOf("a refused upgrade is not used up"));
        leave(helper, player);
        helper.succeed();
    }

    private static void needsSneaking(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.SURVIVAL);
        filledBasicCell(helper);

        use(helper, player, holding(player, NexusItems.ADVANCED_TIER_UPGRADE.get()), false);

        helper.assertValueEqual(helper.getBlockState(CELL).getBlock(), NexusBlocks.BASIC_ENERGY_CELL.get(),
                String.valueOf("the block after a click without sneaking"));
        leave(helper, player);
        helper.succeed();
    }

    private static void notUsedUpInCreative(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.CREATIVE);
        filledBasicCell(helper);
        final ItemStack upgrade = holding(player, NexusItems.ADVANCED_TIER_UPGRADE.get());

        use(helper, player, upgrade, true);

        helper.assertValueEqual(helper.getBlockState(CELL).getBlock(), NexusBlocks.ADVANCED_ENERGY_CELL.get(),
                String.valueOf("the block after the upgrade in creative"));
        helper.assertValueEqual(upgrade.getCount(), 1, String.valueOf("a creative player keeps the upgrade"));
        leave(helper, player);
        helper.succeed();
    }

    private static void networkHoldsMore(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, GameType.SURVIVAL);
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CABLE, cable());
        place(helper, CABLE.east(), NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertValueEqual(
                        helper.<NexusBlockEntity>getBlockEntity(NEXUS).energy().capacity(),
                        EnergyCellTier.BASIC.capacity(), String.valueOf("capacity of the network at first")))
                .thenExecute(() -> useAt(helper, player, CABLE.east(),
                        holding(player, NexusItems.ADVANCED_TIER_UPGRADE.get()), true))
                .thenIdle(2)
                .thenExecute(() -> helper.assertValueEqual(
                        helper.<NexusBlockEntity>getBlockEntity(NEXUS).energy().capacity(),
                        EnergyCellTier.ADVANCED.capacity(), String.valueOf("capacity of the network after")))
                .thenExecute(() -> leave(helper, player))
                .thenSucceed();
    }

    private static void strangerCannotUpgrade(final GameTestHelper helper) {
        final ServerPlayer stranger = player(helper, GameType.SURVIVAL);
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CABLE, cable());
        place(helper, CABLE.east(), NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        final ItemStack upgrade = holding(stranger, NexusItems.ADVANCED_TIER_UPGRADE.get());

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> shutStrangersOut(helper))
                .thenExecute(() -> useAt(helper, stranger, CABLE.east(), upgrade, true))
                .thenExecute(() -> helper.assertValueEqual(helper.getBlockState(CABLE.east()).getBlock(),
                        NexusBlocks.BASIC_ENERGY_CELL.get(), String.valueOf("a stranger upgraded a cell")))
                .thenExecute(() -> helper.assertValueEqual(upgrade.getCount(), 1,
                        String.valueOf("a refused upgrade is not used up")))
                .thenExecute(() -> leave(helper, stranger))
                .thenSucceed();
    }

    private static void shutStrangersOut(final GameTestHelper helper) {
        final NexusBlockEntity nexus = helper.<NexusBlockEntity>getBlockEntity(NEXUS);
        helper.assertValueEqual(nexus.security().apply(Editor.operator(OWNER), new SecurityEdit.Claim("Owner")),
                EditResult.APPLIED, String.valueOf("claiming the network"));
        helper.assertValueEqual(
                nexus.security().apply(Editor.player(OWNER), new SecurityEdit.ChangeDefaultRole(Role.BLOCKED)),
                EditResult.APPLIED, String.valueOf("shutting strangers out"));
    }

    private static EnergyCellBlockEntity filledBasicCell(final GameTestHelper helper) {
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        final EnergyCellBlockEntity cell = helper.<EnergyCellBlockEntity>getBlockEntity(CELL);
        for (long filled = 0; filled < STORED; filled += EnergyCellTier.BASIC.maxTransfer()) {
            cell.energyBuffer().insert(EnergyCellTier.BASIC.maxTransfer(), Action.EXECUTE);
        }
        cell.setEnergyPriority(PRIORITY);
        cell.rename(NAME);
        return cell;
    }

    private static ServerPlayer player(final GameTestHelper helper, final GameType mode) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(mode);
        return player;
    }

    private static ItemStack holding(final ServerPlayer player, final Item item) {
        final ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    private static InteractionResult use(
            final GameTestHelper helper, final ServerPlayer player, final ItemStack stack, final boolean sneaking) {
        return useAt(helper, player, CELL, stack, sneaking);
    }

    private static InteractionResult useAt(
            final GameTestHelper helper, final ServerPlayer player, final BlockPos pos, final ItemStack stack,
            final boolean sneaking) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(sneaking);
        final BlockPos absolute = helper.absolutePos(pos);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    private static List<ItemEntity> itemsOnTheGround(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CELL)).inflate(3));
    }

    private static BlockState cable() {
        return NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState();
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
