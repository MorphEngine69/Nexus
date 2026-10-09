package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.access.AccessRequests;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.level.PlayerActor;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.security.EditResult;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.NetworkSecurity;
import com.morphengine.nexus.security.SecurityEdit;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.transfer.TransferSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Access to a network in a world: who owns a network and what strangers,
 * members and their devices may do with it.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class AccessGameTests {

    private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos DEVICE = CABLE.above();
    private static final BlockPos CHEST = DEVICE.east();
    private static final BlockPos ELSEWHERE = new BlockPos(6, 1, 6);
    private static final UUID OWNER = new UUID(0, 1);
    private static final UUID STRANGER = new UUID(0, 2);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("network_from_before_access_rules_is_open", AccessGameTests::legacyNetworkIsOpen),
            Map.entry("placed_nexus_belongs_to_its_placer", AccessGameTests::placedNexusBelongsToPlacer),
            Map.entry("stranger_takes_nothing_out_of_the_storage", AccessGameTests::strangerTakesNothing),
            Map.entry("pusher_of_a_stranger_stands_still", AccessGameTests::pusherOfStrangerStandsStill),
            Map.entry("pusher_of_a_member_delivers", AccessGameTests::pusherOfMemberDelivers),
            Map.entry("stranger_cannot_break_a_cable", AccessGameTests::strangerCannotBreakCable),
            Map.entry("member_breaks_a_cable", AccessGameTests::memberBreaksCable),
            Map.entry("stranger_cannot_connect_a_cable", AccessGameTests::strangerCannotConnectCable),
            Map.entry("moved_nexus_keeps_its_owner", AccessGameTests::movedNexusKeepsOwner),
            Map.entry("stranger_cannot_place_a_carried_nexus", AccessGameTests::strangerCannotPlaceNexus),
            Map.entry("lone_device_answers_to_its_owner", AccessGameTests::loneDeviceAnswersToOwner),
            Map.entry("only_a_player_on_the_server_can_be_added", AccessGameTests::onlyPlayerOnServerIsAdded));

    private AccessGameTests() {
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        event.register(AccessGameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final List<TestFunction> functions = new ArrayList<>();
        TESTS.forEach((name, test) -> functions.add(new TestFunction(
                "defaultBatch", Nexus.MOD_ID + ":" + name, PLATFORM.toString(), MAX_TICKS, 0, true, test)));
        return functions;
    }

    private static void legacyNetworkIsOpen(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(security(helper).owner().isEmpty(),
                        String.valueOf("a Nexus nobody placed has an owner")))
                .thenExecute(() -> helper.assertTrue(security(helper).isAllowed(STRANGER, Permission.EXTRACT),
                        String.valueOf("a network from before access rules shuts strangers out")))
                .thenSucceed();
    }

    private static void placedNexusBelongsToPlacer(final GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        use(helper, player, new ItemStack(NexusBlocks.NEXUS.get()), NEXUS);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> helper.assertValueEqual(security(helper).owner().orElse(null), player.getUUID(),
                        String.valueOf("owner of the placed Nexus")))
                .thenExecute(() -> helper.assertValueEqual(security(helper).defaultRole(), Role.BLOCKED,
                        String.valueOf("role of strangers in a new network")))
                .thenExecute(() -> leave(helper, player))
                .thenSucceed();
    }

    private static void strangerTakesNothing(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> storage(helper).insert(stone(), 5, Action.EXECUTE, Actor.NOBODY))
                .thenExecute(() -> helper.assertValueEqual(storage(helper).extract(stone(), 5, Action.EXECUTE,
                        new PlayerActor(STRANGER, "Stranger")), 0L, String.valueOf("stone a stranger took")))
                .thenExecute(() -> helper.assertValueEqual(storage(helper).insert(stone(), 1, Action.EXECUTE,
                        new PlayerActor(STRANGER, "Stranger")), 0L, String.valueOf("stone a stranger put in")))
                .thenExecute(() -> helper.assertValueEqual(storage(helper).extract(stone(), 5, Action.EXECUTE,
                        new PlayerActor(OWNER, "Owner")), 5L, String.valueOf("stone the owner took")))
                .thenSucceed();
    }

    private static void pusherOfStrangerStandsStill(final GameTestHelper helper) {
        buildPusher(helper);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> pusher(helper).placedBy(helper.makeMockPlayer(GameType.SURVIVAL)))
                .thenExecute(() -> storage(helper).insert(stone(), 5, Action.EXECUTE, Actor.NOBODY))
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(chest(helper).isEmpty(),
                        String.valueOf("the Pusher of a stranger delivered")))
                .thenExecute(() -> helper.assertValueEqual(pusher(helper).missingPermission(), Permission.EXTRACT,
                        String.valueOf("what the Pusher's owner lacks")))
                .thenSucceed();
    }

    private static void pusherOfMemberDelivers(final GameTestHelper helper) {
        buildPusher(helper);
        final var member = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> edit(helper, new SecurityEdit.AddMember(member.getUUID(), "Member")))
                .thenExecute(() -> pusher(helper).placedBy(member))
                .thenExecute(() -> storage(helper).insert(stone(), 5, Action.EXECUTE, Actor.NOBODY))
                .thenWaitUntil(() -> helper.assertFalse(chest(helper).isEmpty(),
                        String.valueOf("the Pusher of a member delivers nothing")))
                .thenExecute(() -> helper.assertTrue(pusher(helper).missingPermission() == null,
                        String.valueOf("the Pusher of a member stands still")))
                .thenSucceed();
    }

    private static void strangerCannotBreakCable(final GameTestHelper helper) {
        buildNetwork(helper);
        final ServerPlayer stranger = helper.makeMockServerPlayerInLevel();

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> helper.assertTrue(breakingIsRefused(helper, stranger, CABLE),
                        String.valueOf("a stranger broke a cable of the network")))
                .thenExecute(() -> leave(helper, stranger))
                .thenSucceed();
    }

    private static void memberBreaksCable(final GameTestHelper helper) {
        buildNetwork(helper);
        final ServerPlayer member = helper.makeMockServerPlayerInLevel();

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> edit(helper, new SecurityEdit.AddMember(member.getUUID(), "Member")))
                .thenExecute(() -> helper.assertFalse(breakingIsRefused(helper, member, CABLE),
                        String.valueOf("a member may not break a cable of the network")))
                .thenExecute(() -> leave(helper, member))
                .thenSucceed();
    }

    private static void strangerCannotConnectCable(final GameTestHelper helper) {
        buildNetwork(helper);
        final ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        final BlockPos beside = CABLE.east();

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> use(helper, stranger, new ItemStack(NexusBlocks.CABLES.get(DyeColor.BLUE).get()),
                        beside))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, beside))
                .thenExecute(() -> leave(helper, stranger))
                .thenSucceed();
    }

    private static void movedNexusKeepsOwner(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(NEXUS), true))
                .thenExecute(() -> placeFrom(helper, ELSEWHERE, helper.findOneEntity(EntityType.ITEM)))
                .thenIdle(1)
                .thenExecute(() -> helper.assertValueEqual(nexusAt(helper, ELSEWHERE).security().owner().orElse(null),
                        OWNER, String.valueOf("owner of the moved network")))
                .thenSucceed();
    }

    private static void strangerCannotPlaceNexus(final GameTestHelper helper) {
        buildNetwork(helper);
        final ServerPlayer stranger = helper.makeMockServerPlayerInLevel();

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> claim(helper))
                .thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(NEXUS), true))
                .thenExecute(() -> use(helper, stranger, helper.findOneEntity(EntityType.ITEM).getItem(),
                        ELSEWHERE))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, ELSEWHERE))
                .thenExecute(() -> leave(helper, stranger))
                .thenSucceed();
    }

    private static void loneDeviceAnswersToOwner(final GameTestHelper helper) {
        place(helper, DEVICE, NexusBlocks.PUSHER.get().defaultBlockState()
                .setValue(TransferDeviceBlock.FACING, Direction.EAST));
        final var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        pusher(helper).placedBy(owner);

        helper.assertTrue(pusher(helper).accessPolicy().isAllowed(owner.getUUID(), Permission.CONFIGURE),
                String.valueOf("the owner may not configure their own device"));
        helper.assertFalse(pusher(helper).accessPolicy().isAllowed(STRANGER, Permission.OPEN),
                String.valueOf("a stranger may open a device outside every network"));
        helper.succeed();
    }

    private static void onlyPlayerOnServerIsAdded(final GameTestHelper helper) {
        final ServerPlayer editor = helper.makeMockServerPlayerInLevel();
        final ServerPlayer online = helper.makeMockServerPlayerInLevel();

        final SecurityEdit addOnline = AccessRequests.trusted(editor,
                new SecurityEdit.AddMember(online.getUUID(), "Forged"));
        final SecurityEdit addOffline = AccessRequests.trusted(editor,
                new SecurityEdit.AddMember(STRANGER, "Offline"));

        helper.assertValueEqual(addOnline, new SecurityEdit.AddMember(online.getUUID(),
                online.getName().getString()), String.valueOf("adding a player on the server, by their real name"));
        helper.assertTrue(addOffline == null, String.valueOf("a player not on the server could be added"));
        leave(helper, online);
        helper.assertTrue(AccessRequests.trusted(editor, new SecurityEdit.AddMember(online.getUUID(), "Gone")) == null,
                String.valueOf("a player who left could be added"));
        leave(helper, editor);
        helper.succeed();
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

    private static void buildPusher(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, CHEST, Blocks.CHEST.defaultBlockState());
        place(helper, DEVICE, NexusBlocks.PUSHER.get().defaultBlockState()
                .setValue(TransferDeviceBlock.FACING, Direction.EAST));
        pusher(helper).changeSettings(TransferSettings.DEFAULT.withFilter(FilterSlots.EMPTY.with(0, stone())));
    }

    /**
     * Makes {@link #OWNER} the owner of the network, as an operator claims one,
     * and shuts strangers out.
     */
    private static void claim(final GameTestHelper helper) {
        final NetworkSecurity security = security(helper);
        helper.assertValueEqual(security.apply(Editor.operator(OWNER), new SecurityEdit.Claim("Owner")),
                EditResult.APPLIED, String.valueOf("claiming the network"));
        edit(helper, new SecurityEdit.ChangeDefaultRole(Role.BLOCKED));
    }

    private static void edit(final GameTestHelper helper, final SecurityEdit edit) {
        helper.assertValueEqual(security(helper).apply(Editor.player(OWNER), edit), EditResult.APPLIED,
                String.valueOf(edit.toString()));
    }

    private static boolean breakingIsRefused(final GameTestHelper helper, final ServerPlayer player,
                                             final BlockPos pos) {
        final BlockPos absolute = helper.absolutePos(pos);
        return CommonHooks.fireBlockBreak(helper.getLevel(), GameType.SURVIVAL, player, absolute,
                helper.getLevel().getBlockState(absolute)).isCanceled();
    }

    /**
     * Places {@code stack} at {@code pos} as {@code player} does, by a click on the block below.
     */
    private static void use(final GameTestHelper helper, final ServerPlayer player, final ItemStack stack,
                            final BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.setBlock(pos, Blocks.AIR);
        helper.setBlock(pos.below(), Blocks.STONE);
        final BlockPos below = helper.absolutePos(pos).below();
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(below), Direction.UP, below, false)));
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static void placeFrom(final GameTestHelper helper, final BlockPos pos, final ItemEntity drop) {
        helper.setBlock(pos, NexusBlocks.NEXUS.get());
        nexusAt(helper, pos).applyComponentsFromItemStack(drop.getItem());
    }

    private static NetworkSecurity security(final GameTestHelper helper) {
        return nexusAt(helper, NEXUS).security();
    }

    private static NexusBlockEntity nexusAt(final GameTestHelper helper, final BlockPos pos) {
        return helper.<NexusBlockEntity>getBlockEntity(pos);
    }

    private static NetworkStorage storage(final GameTestHelper helper) {
        return nexusAt(helper, NEXUS).component(NetworkComponentTypes.STORAGE).storage();
    }

    private static TransferDeviceBlockEntity pusher(final GameTestHelper helper) {
        return helper.<TransferDeviceBlockEntity>getBlockEntity(DEVICE);
    }

    private static Container chest(final GameTestHelper helper) {
        return helper.<BaseContainerBlockEntity>getBlockEntity(CHEST);
    }

    private static ItemKey stone() {
        return ItemKey.of(new ItemStack(Items.STONE));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
