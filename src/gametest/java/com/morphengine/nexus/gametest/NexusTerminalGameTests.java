package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.NetworkColor;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.item.NexusTerminalItem;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.menu.TerminalSlot;
import com.morphengine.nexus.menu.TerminalSlots;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A Nexus Terminal stays bound to its network: through a rename, when the
 * Nexus is broken and placed elsewhere, and when the Nexus is replaced on the
 * spot. A copy of a Nexus that is still standing starts a network of its own.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class NexusTerminalGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 100;
    private static final int INVENTORY_SLOT = 20;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos ELSEWHERE = new BlockPos(6, 1, 6);
    private static final NetworkColor RED = new NetworkColor(0xC03030);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("terminal_reaches_renamed_network", NexusTerminalGameTests::reachesRenamedNetwork),
            Map.entry("terminal_follows_moved_nexus", NexusTerminalGameTests::followsMovedNexus),
            Map.entry("terminal_takes_nexus_replaced_on_the_spot", NexusTerminalGameTests::takesReplacedNexus),
            Map.entry("copied_nexus_starts_its_own_network", NexusTerminalGameTests::copiedNexusStartsOwnNetwork),
            Map.entry("nexus_broken_in_creative_keeps_its_network",
                    NexusTerminalGameTests::creativeBreakKeepsNetwork),
            Map.entry("default_nexus_broken_in_creative_drops_nothing",
                    NexusTerminalGameTests::creativeBreakOfDefault),
            Map.entry("key_finds_terminal_in_inventory", NexusTerminalGameTests::keyFindsInventoryTerminal),
            Map.entry("key_prefers_hand_to_inventory", NexusTerminalGameTests::keyPrefersHand),
            Map.entry("key_prefers_bound_terminal", NexusTerminalGameTests::keyPrefersBoundTerminal),
            Map.entry("key_finds_nothing_without_terminal", NexusTerminalGameTests::keyFindsNothing));

    private NexusTerminalGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "nexus_terminal"),
                new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void reachesRenamedNetwork(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);
        final ItemStack terminal = boundTerminal(helper, NEXUS);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> nexus(helper, NEXUS).rename("Farm"))
                .thenExecute(() -> assertReaches(helper, terminal, NEXUS))
                .thenSucceed();
    }

    private static void followsMovedNexus(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);
        nexus(helper, NEXUS).rename("Farm");
        nexus(helper, NEXUS).recolor(RED);
        final ItemStack terminal = boundTerminal(helper, NEXUS);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(NEXUS), true))
                .thenExecute(() -> placeFrom(helper, ELSEWHERE, helper.findOneEntity(EntityTypes.ITEM)))
                .thenIdle(1)
                .thenExecute(() -> assertReaches(helper, terminal, ELSEWHERE))
                .thenExecute(() -> helper.assertValueEqual(nexus(helper, ELSEWHERE).network().name(), "Farm",
                        Component.literal("name of the moved network")))
                .thenExecute(() -> helper.assertValueEqual(nexus(helper, ELSEWHERE).network().color(), RED,
                        Component.literal("color of the moved network")))
                .thenSucceed();
    }

    private static void takesReplacedNexus(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);
        final ItemStack terminal = boundTerminal(helper, NEXUS);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> helper.destroyBlock(NEXUS))
                .thenExecute(() -> placeNexus(helper, NEXUS))
                .thenIdle(1)
                .thenExecute(() -> assertReaches(helper, terminal, NEXUS))
                .thenSucceed();
    }

    private static void copiedNexusStartsOwnNetwork(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);
        final ItemStack copy = new ItemStack(NexusBlocks.NEXUS.get());
        copy.applyComponents(nexus(helper, NEXUS).collectComponents());
        final ItemStack terminal = boundTerminal(helper, NEXUS);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> placeFrom(helper, ELSEWHERE, copy))
                .thenIdle(1)
                .thenExecute(() -> helper.assertFalse(
                        nexus(helper, ELSEWHERE).network().id().equals(nexus(helper, NEXUS).network().id()),
                        Component.literal("the copy took over the network of the Nexus it was copied from")))
                .thenExecute(() -> assertReaches(helper, terminal, NEXUS))
                .thenSucceed();
    }

    private static void creativeBreakKeepsNetwork(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);
        nexus(helper, NEXUS).rename("Farm");
        nexus(helper, NEXUS).recolor(RED);
        final UUID id = nexus(helper, NEXUS).network().id();

        breakInCreative(helper, NEXUS);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> placeFrom(helper, ELSEWHERE, helper.findOneEntity(EntityTypes.ITEM)))
                .thenIdle(1)
                .thenExecute(() -> helper.assertValueEqual(nexus(helper, ELSEWHERE).network().id(), id,
                        Component.literal("id of the network of the Nexus broken in creative mode")))
                .thenExecute(() -> helper.assertValueEqual(nexus(helper, ELSEWHERE).network().name(), "Farm",
                        Component.literal("name of the network of the Nexus broken in creative mode")))
                .thenExecute(() -> helper.assertValueEqual(nexus(helper, ELSEWHERE).network().color(), RED,
                        Component.literal("color of the network of the Nexus broken in creative mode")))
                .thenSucceed();
    }

    private static void creativeBreakOfDefault(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);

        breakInCreative(helper, NEXUS);

        helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(),
                Component.literal("a Nexus with the default network dropped in creative mode"));
        helper.succeed();
    }

    private static void keyFindsInventoryTerminal(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(INVENTORY_SLOT, new ItemStack(NexusItems.NEXUS_TERMINAL.get()));

        assertFound(helper, player, new TerminalSlot.Carried(INVENTORY_SLOT));
        helper.succeed();
    }

    private static void keyPrefersHand(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(INVENTORY_SLOT, new ItemStack(NexusItems.NEXUS_TERMINAL.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(NexusItems.NEXUS_TERMINAL.get()));

        assertFound(helper, player, new TerminalSlot.Hand(InteractionHand.OFF_HAND));
        helper.succeed();
    }

    private static void keyPrefersBoundTerminal(final GameTestHelper helper) {
        placeNexus(helper, NEXUS);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NexusItems.NEXUS_TERMINAL.get()));
        player.getInventory().setItem(INVENTORY_SLOT, boundTerminal(helper, NEXUS));

        assertFound(helper, player, new TerminalSlot.Carried(INVENTORY_SLOT));
        helper.succeed();
    }

    private static void keyFindsNothing(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(INVENTORY_SLOT, new ItemStack(NexusItems.WRENCH.get()));

        helper.assertTrue(TerminalSlots.find(player) == null,
                Component.literal("a terminal found in an inventory without one"));
        helper.succeed();
    }

    private static void assertFound(final GameTestHelper helper, final Player player, final TerminalSlot expected) {
        final TerminalSlot found = TerminalSlots.find(player);
        helper.assertTrue(expected.equals(found), Component.literal("the key found " + found + ", not " + expected));
    }

    /**
     * Breaks the block as a creative player does: no drops from the loot table.
     */
    private static void breakInCreative(final GameTestHelper helper, final BlockPos pos) {
        final Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        final BlockPos absolute = helper.absolutePos(pos);
        final BlockState state = helper.getLevel().getBlockState(absolute);
        state.getBlock().playerWillDestroy(helper.getLevel(), absolute, state, player);
        helper.getLevel().removeBlock(absolute, false);
    }

    /**
     * A terminal bound by a right click on the Nexus at {@code pos}, as a player binds it.
     */
    private static ItemStack boundTerminal(final GameTestHelper helper, final BlockPos pos) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack terminal = new ItemStack(NexusItems.NEXUS_TERMINAL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, terminal);
        final BlockPos absolute = helper.absolutePos(pos);
        terminal.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        return terminal;
    }

    private static void assertReaches(final GameTestHelper helper, final ItemStack terminal, final BlockPos pos) {
        final NetworkController reached = NexusTerminalItem.nexusOf(terminal, helper.getLevel().getServer());
        helper.assertTrue(reached == nexus(helper, pos),
                Component.literal("the terminal reaches " + describe(reached) + ", not the Nexus at " + pos));
    }

    private static String describe(final @Nullable NetworkController reached) {
        return reached == null ? "no Nexus" : "the Nexus at " + reached.getBlockPos();
    }

    private static void placeFrom(final GameTestHelper helper, final BlockPos pos, final ItemEntity drop) {
        placeFrom(helper, pos, drop.getItem());
    }

    private static void placeFrom(final GameTestHelper helper, final BlockPos pos, final ItemStack stack) {
        placeNexus(helper, pos);
        nexus(helper, pos).applyComponentsFromItemStack(stack);
    }

    private static void placeNexus(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, NexusBlocks.NEXUS.get());
    }

    private static NexusBlockEntity nexus(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, NexusBlockEntity.class);
    }
}
