package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.network.security.Role;
import com.morphengine.nexus.block.CableBlock;
import com.morphengine.nexus.block.ExternalVaultBlock;
import com.morphengine.nexus.block.GeneratorBlock;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.SideConnections;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.StoredFluids;
import com.morphengine.nexus.item.WrenchActions;
import com.morphengine.nexus.machine.InputMode;
import com.morphengine.nexus.machine.MachineSide;
import com.morphengine.nexus.processing.MachineFacing;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.security.EditResult;
import com.morphengine.nexus.security.Editor;
import com.morphengine.nexus.security.SecurityEdit;
import com.morphengine.nexus.transport.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The Wrench in a world: turning blocks, setting their front, and taking them down into the inventory.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class WrenchGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos DEVICE = new BlockPos(4, 2, 4);
    private static final UUID OWNER = new UUID(0, 1);
    private static final int DROP_SEARCH_RADIUS = 3;
    private static final long STORED_FE = 4_321;
    private static final int STORED_FLUID = 2_500;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("wrench_turns_a_pusher_round_all_six_sides", WrenchGameTests::turnsAroundAllSides),
            Map.entry("wrench_turn_skips_the_sides_a_vault_cannot_face", WrenchGameTests::turnSkipsImpossibleSides),
            Map.entry("wrench_turn_drops_the_cable_arm_on_the_new_front", WrenchGameTests::turnDropsCableArm),
            Map.entry("wrench_left_click_sets_the_front_to_the_struck_side", WrenchGameTests::leftClickSetsFront),
            Map.entry("wrench_left_click_on_an_impossible_side_changes_nothing", WrenchGameTests::leftClickImpossible),
            Map.entry("wrench_does_not_turn_a_terminal", WrenchGameTests::terminalIsNotTurned),
            Map.entry("wrench_dismantle_puts_the_block_and_its_cells_into_the_inventory",
                    WrenchGameTests::dismantleFillsInventory),
            Map.entry("wrench_dismantle_leaves_the_cable_under_a_terminal", WrenchGameTests::dismantleTerminal),
            Map.entry("wrench_dismantle_drops_what_does_not_fit", WrenchGameTests::dismantleOverflowsToTheGround),
            Map.entry("stranger_cannot_turn_a_device_of_a_network", WrenchGameTests::strangerCannotTurn),
            Map.entry("wrench_turns_a_machine_round_the_horizontal", WrenchGameTests::turnsMachine),
            Map.entry("wrench_turns_a_generator_round_the_horizontal", WrenchGameTests::turnsGenerator),
            Map.entry("wrench_turns_an_external_vault_round_all_six_sides", WrenchGameTests::turnsExternalVault),
            Map.entry("a_turned_machine_keeps_its_settings_and_its_sides_follow_the_front",
                    WrenchGameTests::turnedMachineKeepsSettings),
            Map.entry("wrench_dismantle_gives_back_a_machine_and_its_upgrades", WrenchGameTests::dismantleMachine),
            Map.entry("wrench_dismantle_gives_back_a_generator_and_its_contents",
                    WrenchGameTests::dismantleGenerator),
            Map.entry("wrench_dismantle_gives_back_an_external_vault_and_its_upgrades",
                    WrenchGameTests::dismantleExternalVault),
            Map.entry("a_machine_taken_down_with_the_wrench_keeps_its_energy", WrenchGameTests::machineKeepsEnergy),
            Map.entry("an_extractor_taken_down_with_the_wrench_keeps_its_fluid",
                    WrenchGameTests::extractorKeepsFluid),
            Map.entry("a_generator_taken_down_with_the_wrench_keeps_its_energy_and_fluid",
                    WrenchGameTests::generatorKeepsEnergyAndFluid),
            Map.entry("a_machine_without_energy_is_put_up_empty", WrenchGameTests::emptyMachineStaysEmpty));

    private WrenchGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "wrench"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void turnsAroundAllSides(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, pusherFacing(Direction.NORTH));

        final List<Direction> expected = List.of(Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP,
                Direction.DOWN, Direction.NORTH);
        for (Direction side : expected) {
            click(helper, player, DEVICE, false);
            helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(TransferDeviceBlock.FACING), side,
                    Component.literal("facing after a turn"));
        }
        leave(helper, player);
        helper.succeed();
    }

    private static void turnSkipsImpossibleSides(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, vaultFacing(Direction.WEST));

        click(helper, player, DEVICE, false);

        helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(StorageVaultBlock.FACING), Direction.NORTH,
                Component.literal("a vault turned from west goes on round the horizontal"));
        leave(helper, player);
        helper.succeed();
    }

    private static void turnDropsCableArm(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE.east(), cable());
        place(helper, DEVICE, pusherFacing(Direction.NORTH));
        helper.assertTrue(SideConnections.isAttached(helper.getBlockState(DEVICE), Direction.EAST),
                Component.literal("the cable at the side is not joined before turning"));

        click(helper, player, DEVICE, false);

        helper.assertFalse(SideConnections.isAttached(helper.getBlockState(DEVICE), Direction.EAST),
                Component.literal("the cable arm stays on the new front"));
        leave(helper, player);
        helper.succeed();
    }

    private static void leftClickSetsFront(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, pusherFacing(Direction.NORTH));

        final InteractionResult result = WrenchActions.turn(
                helper.getLevel(), helper.absolutePos(DEVICE), player, Direction.UP);

        helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(TransferDeviceBlock.FACING), Direction.UP,
                Component.literal("facing after striking the top"));
        helper.assertTrue(result.consumesAction(), Component.literal("setting a front is an action"));
        leave(helper, player);
        helper.succeed();
    }

    private static void leftClickImpossible(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, vaultFacing(Direction.WEST));

        final InteractionResult result = WrenchActions.turn(
                helper.getLevel(), helper.absolutePos(DEVICE), player, Direction.UP);

        helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(StorageVaultBlock.FACING), Direction.WEST,
                Component.literal("a vault struck on its top keeps its front"));
        helper.assertFalse(result.consumesAction(), Component.literal("a refused turn is not an action"));
        leave(helper, player);
        helper.succeed();
    }

    private static void terminalIsNotTurned(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE.below(), cable());
        place(helper, DEVICE, terminalFacing(Direction.UP));

        final InteractionResult result = click(helper, player, DEVICE, false);

        helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(TerminalBlock.FACING), Direction.UP,
                Component.literal("a terminal stays as it is"));
        helper.assertValueEqual(result, InteractionResult.PASS,
                Component.literal("the click goes on to the terminal, which opens"));
        leave(helper, player);
        helper.succeed();
    }

    private static void dismantleFillsInventory(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        final ItemStack cell = new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get());
        place(helper, DEVICE, NexusBlocks.STORAGE_VAULT.get().defaultBlockState());
        helper.getBlockEntity(DEVICE, StorageVaultBlockEntity.class).cells().setItem(0, cell.copy());

        click(helper, player, DEVICE, true);

        helper.assertTrue(helper.getBlockState(DEVICE).isAir(), Component.literal("the vault is still there"));
        helper.assertTrue(carries(player, NexusItems.STORAGE_VAULT.get()),
                Component.literal("the vault is not in the inventory"));
        helper.assertTrue(carries(player, cell.getItem()), Component.literal("the cell is not in the inventory"));
        helper.assertTrue(itemsOnTheGround(helper).isEmpty(), Component.literal("something fell to the ground"));
        leave(helper, player);
        helper.succeed();
    }

    private static void dismantleTerminal(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE.below(), cable());
        place(helper, DEVICE, terminalFacing(Direction.UP));

        click(helper, player, DEVICE, true);

        helper.assertTrue(helper.getBlockState(DEVICE).isAir(), Component.literal("the terminal is still there"));
        helper.assertTrue(helper.getBlockState(DEVICE.below()).getBlock() instanceof CableBlock,
                Component.literal("the cable under the terminal went with it"));
        helper.assertTrue(carries(player, NexusItems.TERMINAL.get()),
                Component.literal("the terminal is not in the inventory"));
        leave(helper, player);
        helper.succeed();
    }

    private static void dismantleOverflowsToTheGround(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.DIRT, Items.DIRT.getDefaultMaxStackSize()));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NexusItems.WRENCH.get()));
        place(helper, DEVICE, NexusBlocks.STORAGE_VAULT.get().defaultBlockState());

        click(helper, player, DEVICE, true);

        helper.assertTrue(helper.getBlockState(DEVICE).isAir(), Component.literal("the vault is still there"));
        helper.assertFalse(itemsOnTheGround(helper).isEmpty(),
                Component.literal("with a full inventory the vault must fall beside the block"));
        leave(helper, player);
        helper.succeed();
    }

    private static void strangerCannotTurn(final GameTestHelper helper) {
        final BlockPos pusher = NEXUS.east().east();
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, NEXUS.east(), cable());
        place(helper, pusher, pusherFacing(Direction.NORTH));
        final ServerPlayer stranger = survivalPlayer(helper);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> shutStrangersOut(helper))
                .thenExecute(() -> click(helper, stranger, pusher, false))
                .thenExecute(() -> helper.assertValueEqual(
                        helper.getBlockState(pusher).getValue(TransferDeviceBlock.FACING), Direction.NORTH,
                        Component.literal("a stranger turned a device of the network")))
                .thenExecute(() -> leave(helper, stranger))
                .thenSucceed();
    }

    private static void turnsMachine(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, machineFacing(Direction.NORTH));

        for (Direction side : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH)) {
            click(helper, player, DEVICE, false);
            helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(MachineBlock.FACING), side,
                    Component.literal("facing of a machine after a turn"));
        }
        leave(helper, player);
        helper.succeed();
    }

    private static void turnsGenerator(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, generatorFacing(Direction.NORTH));

        for (Direction side : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH)) {
            click(helper, player, DEVICE, false);
            helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(GeneratorBlock.FACING), side,
                    Component.literal("facing of a generator after a turn"));
        }
        leave(helper, player);
        helper.succeed();
    }

    private static void turnsExternalVault(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, externalVaultFacing(Direction.NORTH));

        for (Direction side : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP,
                Direction.DOWN, Direction.NORTH)) {
            click(helper, player, DEVICE, false);
            helper.assertValueEqual(helper.getBlockState(DEVICE).getValue(ExternalVaultBlock.FACING), side,
                    Component.literal("facing of an external vault after a turn"));
        }
        leave(helper, player);
        helper.succeed();
    }

    private static void turnedMachineKeepsSettings(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, machineFacing(Direction.NORTH));
        final MachineBlockEntity machine = helper.getBlockEntity(DEVICE, MachineBlockEntity.class);
        machine.setSideMode(MachineSide.LEFT, SideMode.CLOSED);
        machine.machine().inventory().setMode(InputMode.SPLIT);
        helper.assertValueEqual(closedHorizontalSides(machine).size(), 1,
                Component.literal("sides closed before the turn"));

        click(helper, player, DEVICE, false);

        final Direction facing = helper.getBlockState(DEVICE).getValue(MachineBlock.FACING);
        final List<Direction> closed = closedHorizontalSides(helper.getBlockEntity(DEVICE, MachineBlockEntity.class));
        helper.assertValueEqual(closed.size(), 1, Component.literal("sides closed after the turn"));
        helper.assertValueEqual(MachineFacing.sideOf(facing, closed.getFirst()), MachineSide.LEFT,
                Component.literal("the side that stayed closed, counted from the new front"));
        helper.assertValueEqual(machine.machine().inventory().mode(), InputMode.SPLIT,
                Component.literal("input mode after the turn"));
        leave(helper, player);
        helper.succeed();
    }

    private static List<Direction> closedHorizontalSides(final MachineBlockEntity machine) {
        return Direction.Plane.HORIZONTAL.stream().filter(side -> machine.itemHandler(side) == null).toList();
    }

    private static void dismantleMachine(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, machineFacing(Direction.NORTH));
        final Item machineItem = helper.getBlockState(DEVICE).getBlock().asItem();
        helper.getBlockEntity(DEVICE, MachineBlockEntity.class).upgrades()
                .setItem(0, new ItemStack(NexusItems.SPEED_UPGRADE.get()));

        click(helper, player, DEVICE, true);

        helper.assertTrue(helper.getBlockState(DEVICE).isAir(), Component.literal("the machine is still there"));
        helper.assertTrue(carries(player, machineItem), Component.literal("the machine is not in the inventory"));
        helper.assertTrue(carries(player, NexusItems.SPEED_UPGRADE.get()),
                Component.literal("the upgrade is not in the inventory"));
        helper.assertTrue(itemsOnTheGround(helper).isEmpty(), Component.literal("something fell to the ground"));
        leave(helper, player);
        helper.succeed();
    }

    private static void dismantleGenerator(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, generatorFacing(Direction.NORTH));
        final Item generatorItem = helper.getBlockState(DEVICE).getBlock().asItem();
        final GeneratorBlockEntity generator = helper.getBlockEntity(DEVICE, GeneratorBlockEntity.class);
        generator.input().setItem(0, new ItemStack(Items.COAL, 5));
        generator.upgrades().setItem(0, new ItemStack(NexusItems.SPEED_UPGRADE.get()));

        click(helper, player, DEVICE, true);

        helper.assertTrue(helper.getBlockState(DEVICE).isAir(), Component.literal("the generator is still there"));
        helper.assertTrue(carries(player, generatorItem), Component.literal("the generator is not in the inventory"));
        helper.assertTrue(carries(player, Items.COAL), Component.literal("the fuel is not in the inventory"));
        helper.assertTrue(carries(player, NexusItems.SPEED_UPGRADE.get()),
                Component.literal("the upgrade is not in the inventory"));
        helper.assertTrue(itemsOnTheGround(helper).isEmpty(), Component.literal("something fell to the ground"));
        leave(helper, player);
        helper.succeed();
    }

    private static void dismantleExternalVault(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, externalVaultFacing(Direction.NORTH));
        helper.getBlockEntity(DEVICE, ExternalVaultBlockEntity.class).upgrades()
                .setItem(0, new ItemStack(NexusItems.CHUNK_LOADER_UPGRADE.get()));

        click(helper, player, DEVICE, true);

        helper.assertTrue(helper.getBlockState(DEVICE).isAir(), Component.literal("the vault is still there"));
        helper.assertTrue(carries(player, NexusItems.EXTERNAL_VAULT.get()),
                Component.literal("the vault is not in the inventory"));
        helper.assertTrue(carries(player, NexusItems.CHUNK_LOADER_UPGRADE.get()),
                Component.literal("the upgrade is not in the inventory"));
        leave(helper, player);
        helper.succeed();
    }

    private static BlockState machineFacing(final Direction facing) {
        return NexusBlocks.machineTiers(MachineKind.CRUSHER).getFirst().get().defaultBlockState()
                .setValue(MachineBlock.FACING, facing);
    }

    private static BlockState generatorFacing(final Direction facing) {
        return NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get().defaultBlockState()
                .setValue(GeneratorBlock.FACING, facing);
    }

    private static BlockState externalVaultFacing(final Direction facing) {
        return NexusBlocks.EXTERNAL_VAULT.get().defaultBlockState().setValue(ExternalVaultBlock.FACING, facing);
    }

    private static void machineKeepsEnergy(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, machineFacing(Direction.NORTH));
        final Item machineItem = helper.getBlockState(DEVICE).getBlock().asItem();
        seed(helper.getBlockEntity(DEVICE, MachineBlockEntity.class), STORED_FE, StoredFluids.EMPTY);

        click(helper, player, DEVICE, true);
        final ItemStack dropped = carried(player, machineItem);
        place(helper, DEVICE.east(), machineFacing(Direction.NORTH));
        helper.getBlockEntity(DEVICE.east(), MachineBlockEntity.class).applyComponentsFromItemStack(dropped);

        helper.assertValueEqual(dropped.get(NexusDataComponents.STORED_ENERGY.get()), STORED_FE,
                Component.literal("energy on the item"));
        helper.assertValueEqual(helper.getBlockEntity(DEVICE.east(), MachineBlockEntity.class)
                .machine().energy().stored(), STORED_FE, Component.literal("energy in the machine put up again"));
        leave(helper, player);
        helper.succeed();
    }

    private static void emptyMachineStaysEmpty(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        place(helper, DEVICE, machineFacing(Direction.NORTH));
        final Item machineItem = helper.getBlockState(DEVICE).getBlock().asItem();

        click(helper, player, DEVICE, true);
        final ItemStack dropped = carried(player, machineItem);
        place(helper, DEVICE.east(), machineFacing(Direction.NORTH));
        helper.getBlockEntity(DEVICE.east(), MachineBlockEntity.class).applyComponentsFromItemStack(dropped);

        helper.assertValueEqual(helper.getBlockEntity(DEVICE.east(), MachineBlockEntity.class)
                .machine().energy().stored(), 0L, Component.literal("energy in a machine that held none"));
        leave(helper, player);
        helper.succeed();
    }

    private static void extractorKeepsFluid(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        final BlockState extractor = NexusBlocks.machineTiers(MachineKind.EXTRACTOR).getFirst().get()
                .defaultBlockState();
        place(helper, DEVICE, extractor);
        final Item extractorItem = extractor.getBlock().asItem();
        final StoredFluids fluid = new StoredFluids(List.of(new FluidStack(Fluids.WATER, STORED_FLUID)));
        seed(helper.getBlockEntity(DEVICE, MachineBlockEntity.class), STORED_FE, fluid);

        click(helper, player, DEVICE, true);
        final ItemStack dropped = carried(player, extractorItem);
        place(helper, DEVICE.east(), extractor);
        helper.getBlockEntity(DEVICE.east(), MachineBlockEntity.class).applyComponentsFromItemStack(dropped);

        helper.assertValueEqual(dropped.get(NexusDataComponents.STORED_FLUIDS.get()), fluid,
                Component.literal("fluid on the item"));
        helper.assertValueEqual(helper.getBlockEntity(DEVICE.east(), MachineBlockEntity.class)
                .collectComponents().get(NexusDataComponents.STORED_FLUIDS.get()), fluid,
                Component.literal("fluid in the extractor put up again"));
        leave(helper, player);
        helper.succeed();
    }

    private static void generatorKeepsEnergyAndFluid(final GameTestHelper helper) {
        final ServerPlayer player = survivalPlayer(helper);
        final BlockState generator = NexusBlocks.GENERATORS.get(GeneratorKind.STEAM).get().defaultBlockState();
        place(helper, DEVICE, generator);
        final Item generatorItem = generator.getBlock().asItem();
        final StoredFluids fluids = new StoredFluids(List.of(
                new FluidStack(Fluids.LAVA, STORED_FLUID), new FluidStack(Fluids.WATER, STORED_FLUID)));
        seed(helper.getBlockEntity(DEVICE, GeneratorBlockEntity.class), STORED_FE, fluids);

        click(helper, player, DEVICE, true);
        final ItemStack dropped = carried(player, generatorItem);
        place(helper, DEVICE.east(), generator);
        helper.getBlockEntity(DEVICE.east(), GeneratorBlockEntity.class).applyComponentsFromItemStack(dropped);

        final DataComponentMap again = helper.getBlockEntity(DEVICE.east(), GeneratorBlockEntity.class)
                .collectComponents();
        helper.assertValueEqual(again.get(NexusDataComponents.STORED_ENERGY.get()), STORED_FE,
                Component.literal("energy in the generator put up again"));
        helper.assertValueEqual(again.get(NexusDataComponents.STORED_FLUIDS.get()), fluids,
                Component.literal("fluids in the generator put up again"));
        leave(helper, player);
        helper.succeed();
    }

    private static void seed(final BlockEntity device, final long energy, final StoredFluids fluids) {
        final ItemStack stack = new ItemStack(Items.STONE);
        stack.set(NexusDataComponents.STORED_ENERGY.get(), energy);
        stack.set(NexusDataComponents.STORED_FLUIDS.get(), fluids);
        device.applyComponentsFromItemStack(stack);
    }

    private static ItemStack carried(final ServerPlayer player, final Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return stack;
            }
        }
        throw new AssertionError("the inventory holds no " + item);
    }

    private static void shutStrangersOut(final GameTestHelper helper) {
        final NexusBlockEntity nexus = helper.getBlockEntity(NEXUS, NexusBlockEntity.class);
        helper.assertValueEqual(nexus.security().apply(Editor.operator(OWNER), new SecurityEdit.Claim("Owner")),
                EditResult.APPLIED, Component.literal("claiming the network"));
        helper.assertValueEqual(
                nexus.security().apply(Editor.player(OWNER), new SecurityEdit.ChangeDefaultRole(Role.BLOCKED)),
                EditResult.APPLIED, Component.literal("shutting strangers out"));
    }

    private static ServerPlayer survivalPlayer(final GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NexusItems.WRENCH.get()));
        return player;
    }

    private static InteractionResult click(
            final GameTestHelper helper, final ServerPlayer player, final BlockPos pos, final boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        final BlockPos absolute = helper.absolutePos(pos);
        final ItemStack wrench = player.getItemInHand(InteractionHand.MAIN_HAND);
        return wrench.getItem().onItemUseFirst(wrench, new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    private static boolean carries(final ServerPlayer player, final Item item) {
        return player.getInventory().contains(stack -> stack.is(item));
    }

    private static List<ItemEntity> itemsOnTheGround(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, new AABB(helper.absolutePos(DEVICE)).inflate(DROP_SEARCH_RADIUS));
    }

    private static BlockState pusherFacing(final Direction facing) {
        return NexusBlocks.PUSHER.get().defaultBlockState().setValue(TransferDeviceBlock.FACING, facing);
    }

    private static BlockState vaultFacing(final Direction facing) {
        return NexusBlocks.STORAGE_VAULT.get().defaultBlockState().setValue(StorageVaultBlock.FACING, facing);
    }

    private static BlockState terminalFacing(final Direction facing) {
        return NexusBlocks.TERMINAL.get().defaultBlockState().setValue(TerminalBlock.FACING, facing);
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
