package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TerminalBlock;
import com.morphengine.nexus.block.VaultLamp;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TerminalBlockEntity;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.menu.BlockTerminalBinding;
import com.morphengine.nexus.menu.CraftingTerminalMenu;
import com.morphengine.nexus.menu.TerminalOpening;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.NetworkStorage;
import com.morphengine.nexus.terminal.GridFill;
import com.morphengine.nexus.terminal.TerminalSettings;
import com.morphengine.nexus.terminal.TerminalStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The network's storage in a world: Storage Vaults lending their cells at their
 * priority, cell filters, lamps and terminals.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class StorageGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("vault_lends_cells_to_network", StorageGameTests::vaultLendsCellsToNetwork),
            Map.entry("higher_priority_vault_fills_first", StorageGameTests::higherPriorityVaultFillsFirst),
            Map.entry("whitelisted_cell_takes_only_listed", StorageGameTests::whitelistedCellTakesOnlyListed),
            Map.entry("broken_vault_drops_cells_with_contents", StorageGameTests::brokenVaultDropsCells),
            Map.entry("vault_saved_with_more_slots_keeps_every_cell", StorageGameTests::vaultKeepsCellsPastItsSlots),
            Map.entry("vault_lamps_follow_energy", StorageGameTests::vaultLampsFollowEnergy),
            Map.entry("terminal_goes_online_with_energy", StorageGameTests::terminalGoesOnlineWithEnergy),
            Map.entry("terminal_falls_off_without_cable", StorageGameTests::terminalFallsOffWithoutCable),
            Map.entry("recipe_for_most_crafts_splits_ingredients", StorageGameTests::recipeSplitsIngredients));

    private StorageGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "storage"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void vaultLendsCellsToNetwork(final GameTestHelper helper) {
        final BlockPos vaultPos = NEXUS.east(2);
        buildLine(helper, cable(), vault());
        vaultEntity(helper, vaultPos).cells().setItem(0, cell(CellTier.ONE_K));

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, stone(), 100, 100))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(stone()), 100, "network stone"))
                .thenSucceed();
    }

    private static void higherPriorityVaultFillsFirst(final GameTestHelper helper) {
        final BlockPos low = NEXUS.east(2);
        final BlockPos high = NEXUS.south();
        buildLine(helper, cable(), vault());
        place(helper, high, vault().setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        vaultEntity(helper, low).cells().setItem(0, cell(CellTier.ONE_K));
        vaultEntity(helper, high).cells().setItem(0, cell(CellTier.ONE_K));
        vaultEntity(helper, high).setPriority(5);

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, stone(), 10, 10))
                .thenIdle(21)
                .thenExecute(() -> {
                    final List<?> highContents = VaultCellItem.contentsOf(vaultEntity(helper, high).cells().getItem(0));
                    final List<?> lowContents = VaultCellItem.contentsOf(vaultEntity(helper, low).cells().getItem(0));
                    helper.assertTrue(highContents.size() == 1 && lowContents.isEmpty(),
                            Component.literal("high vault holds " + highContents + ", low vault " + lowContents));
                })
                .thenSucceed();
    }

    private static void whitelistedCellTakesOnlyListed(final GameTestHelper helper) {
        final ItemStack stoneCell = cell(CellTier.ONE_K);
        stoneCell.set(NexusDataComponents.CELL_FILTER.get(),
                FilterSlots.EMPTY.with(0, stone()).withMode(FilterMode.ALLOW));
        buildLine(helper, cable(), vault());
        vaultEntity(helper, NEXUS.east(2)).cells().setItem(0, stoneCell);

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, stone(), 10, 10))
                .thenExecute(() -> assertAmount(helper,
                        network(helper).insert(dirt(), 10, Action.EXECUTE, Actor.NOBODY), 0, "dirt accepted"))
                .thenSucceed();
    }

    private static void brokenVaultDropsCells(final GameTestHelper helper) {
        final BlockPos vaultPos = NEXUS.east(2);
        buildLine(helper, cable(), vault());
        vaultEntity(helper, vaultPos).cells().setItem(0, cell(CellTier.ONE_K));

        helper.startSequence()
                .thenWaitUntil(() -> assertInserted(helper, stone(), 64, 64))
                .thenExecute(() -> helper.destroyBlock(vaultPos))
                .thenWaitUntil(() -> helper.assertItemEntityPresent(
                        NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get()))
                .thenExecute(() -> {
                    final ItemStack dropped = helper.getEntities(EntityTypes.ITEM).stream()
                            .map(ItemEntity::getItem)
                            .filter(stack -> stack.getItem() instanceof VaultCellItem)
                            .findFirst().orElse(ItemStack.EMPTY);
                    final long stone = VaultCellItem.contentsOf(dropped).stream()
                            .filter(amount -> amount.resource().equals(stone())).mapToLong(a -> a.amount()).sum();
                    assertAmount(helper, stone, 64, "stone on the dropped cell");
                    assertAmount(helper, network(helper).amountOf(stone()), 0, "network stone after the vault broke");
                })
                .thenSucceed();
    }

    /**
     * A vault saved while it had 32 slots, with 17 cells at slots 1 to 17: the
     * cell at slot 16 moves into the empty slot 0, the one at 17 is dropped.
     */
    private static void vaultKeepsCellsPastItsSlots(final GameTestHelper helper) {
        final BlockPos vaultPos = NEXUS.east(2);
        place(helper, vaultPos, vault());
        final RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        final ListTag items = new ListTag();
        for (int slot = 1; slot <= StorageVaultBlockEntity.SLOTS + 1; slot++) {
            items.add(ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(slot, cell(CellTier.ONE_K)))
                    .getOrThrow());
        }
        final CompoundTag saved = new CompoundTag();
        saved.put(ContainerHelper.TAG_ITEMS, items);
        vaultEntity(helper, vaultPos).loadWithComponents(
                TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertItemEntityCountIs(
                        cell(CellTier.ONE_K).getItem(), vaultPos.north(), 2, 1))
                .thenExecute(() -> {
                    final Container cells = vaultEntity(helper, vaultPos).cells();
                    int empty = 0;
                    for (int slot = 0; slot < cells.getContainerSize(); slot++) {
                        empty += cells.getItem(slot).isEmpty() ? 1 : 0;
                    }
                    assertAmount(helper, empty, 0, "empty slots of the vault");
                })
                .thenSucceed();
    }

    private static void vaultLampsFollowEnergy(final GameTestHelper helper) {
        final BlockPos vaultPos = NEXUS.east(2);
        final BlockPos cellPos = NEXUS.south();
        buildLine(helper, cable(), vault());
        place(helper, cellPos, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        vaultEntity(helper, vaultPos).cells().setItem(0, cell(CellTier.ONE_K));

        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> assertLamp(helper, vaultPos, VaultLamp.OFF))
                .thenExecute(() -> charge(helper, cellPos))
                .thenWaitUntil(() -> assertLamp(helper, vaultPos, VaultLamp.GREEN))
                .thenExecute(() -> {
                    final VaultLamp empty = vaultEntity(helper, vaultPos).lampAt(1);
                    helper.assertTrue(empty == VaultLamp.OFF, Component.literal("empty bay shows " + empty));
                })
                .thenSucceed();
    }

    private static void terminalGoesOnlineWithEnergy(final GameTestHelper helper) {
        final BlockPos cellPos = NEXUS.south();
        final BlockPos terminalPos = NEXUS.east().above();
        buildLine(helper, cable());
        place(helper, cellPos, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        place(helper, terminalPos, terminal(Direction.UP));

        helper.startSequence()
                .thenWaitUntil(() -> assertStatus(helper, terminalPos, TerminalStatus.NO_ENERGY))
                .thenExecute(() -> charge(helper, cellPos))
                .thenWaitUntil(() -> {
                    assertStatus(helper, terminalPos, TerminalStatus.ONLINE);
                    helper.assertTrue(helper.getBlockState(terminalPos).getValue(TerminalBlock.POWERED),
                            Component.literal("terminal screen is dark with energy in the network"));
                })
                .thenSucceed();
    }

    private static void terminalFallsOffWithoutCable(final GameTestHelper helper) {
        final BlockPos cablePos = new BlockPos(2, 1, 2);
        final BlockPos terminalPos = cablePos.above();
        place(helper, cablePos, cable());
        place(helper, terminalPos, terminal(Direction.UP));

        helper.startSequence()
                .thenExecute(() -> helper.destroyBlock(cablePos))
                .thenWaitUntil(() -> helper.assertBlockNotPresent(NexusBlocks.TERMINAL.get(), terminalPos))
                .thenSucceed();
    }

    /**
     * A crafting table asked for as many times as possible from eight planks:
     * two crafts, so two planks on each of the four slots, not all on the first.
     */
    private static void recipeSplitsIngredients(final GameTestHelper helper) {
        final BlockPos cablePos = new BlockPos(2, 1, 2);
        final BlockPos terminalPos = cablePos.above();
        place(helper, cablePos, cable());
        place(helper, terminalPos, NexusBlocks.CRAFTING_TERMINAL.get().defaultBlockState()
                .setValue(TerminalBlock.FACING, Direction.UP));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().add(new ItemStack(Items.OAK_PLANKS, 8));
        final CraftingTerminalMenu menu = new CraftingTerminalMenu(NexusMenuTypes.CRAFTING_TERMINAL.get(), 1,
                player.getInventory(), new TerminalOpening(
                        new BlockTerminalBinding(player.getInventory(), helper.absolutePos(terminalPos)),
                        TerminalSettings.DEFAULT));
        final List<ItemKey> planks = List.of(ItemKey.of(new ItemStack(Items.OAK_PLANKS)));
        menu.fillGrid(List.of(planks, planks, List.of(), planks, planks, List.of(), List.of(), List.of(), List.of()),
                GridFill.MOST_CRAFTS);

        for (int slot : new int[] {0, 1, 3, 4}) {
            assertAmount(helper, menu.slots.get(1 + slot).getItem().getCount(), 2, "planks on grid slot " + slot);
        }
        helper.succeed();
    }

    private static void assertInserted(
            final GameTestHelper helper, final ItemKey resource, final long amount, final long expected) {
        assertAmount(helper, network(helper).insert(resource, amount, Action.EXECUTE, Actor.NOBODY), expected,
                "accepted " + resource.name().getString());
    }

    private static void assertAmount(final GameTestHelper helper, final long actual, final long expected,
                                     final String what) {
        helper.assertTrue(actual == expected, Component.literal(what + ": " + actual + ", expected " + expected));
    }

    private static void assertLamp(final GameTestHelper helper, final BlockPos pos, final VaultLamp expected) {
        final VaultLamp shown = vaultEntity(helper, pos).lampAt(0);
        helper.assertTrue(shown == expected, Component.literal("lamp shows " + shown + ", expected " + expected));
    }

    private static void assertStatus(final GameTestHelper helper, final BlockPos pos, final TerminalStatus expected) {
        final TerminalStatus status = helper.getBlockEntity(pos, TerminalBlockEntity.class).status();
        helper.assertTrue(status == expected, Component.literal("terminal " + status + ", expected " + expected));
    }

    /**
     * Charges the energy cell at {@code pos}, if there is one.
     */
    private static void charge(final GameTestHelper helper, final BlockPos pos) {
        final EnergyHandler handler = helper.getLevel()
                .getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.WEST);
        if (handler != null) {
            try (Transaction transaction = Transaction.openRoot()) {
                handler.insert(10_000, transaction);
                transaction.commit();
            }
        }
    }

    private static ItemKey stone() {
        return ItemKey.of(new ItemStack(Items.STONE));
    }

    private static ItemKey dirt() {
        return ItemKey.of(new ItemStack(Items.DIRT));
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.getBlockEntity(NEXUS, NexusBlockEntity.class)
                .component(NetworkComponentTypes.STORAGE).storage();
    }

    private static StorageVaultBlockEntity vaultEntity(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, StorageVaultBlockEntity.class);
    }

    private static ItemStack cell(final CellTier tier) {
        return new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(tier).get());
    }

    private static BlockState cable() {
        return NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState();
    }

    private static BlockState vault() {
        return NexusBlocks.STORAGE_VAULT.get().defaultBlockState().setValue(StorageVaultBlock.FACING, Direction.NORTH);
    }

    private static BlockState terminal(final Direction facing) {
        return NexusBlocks.TERMINAL.get().defaultBlockState().setValue(TerminalBlock.FACING, facing);
    }

    private static void buildLine(final GameTestHelper helper, final BlockState... eastOfNexus) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        for (int i = 0; i < eastOfNexus.length; i++) {
            place(helper, NEXUS.east(i + 1), eastOfNexus[i]);
        }
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
