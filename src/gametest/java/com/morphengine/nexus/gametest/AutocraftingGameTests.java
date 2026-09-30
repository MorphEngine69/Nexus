package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.assembler.LockMode;
import com.morphengine.nexus.block.AssemblerBlock;
import com.morphengine.nexus.block.StorageVaultBlock;
import com.morphengine.nexus.block.TransferDeviceBlock;
import com.morphengine.nexus.block.entity.AssemblerBlockEntity;
import com.morphengine.nexus.block.entity.BlueprintEncoder;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.blueprint.BlueprintDraft;
import com.morphengine.nexus.blueprint.CraftingBlueprint;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.blueprint.GridSlot;
import com.morphengine.nexus.blueprint.ProcessingBlueprint;
import com.morphengine.nexus.blueprint.ProcessingInput;
import com.morphengine.nexus.blueprint.Substitution;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.item.BlueprintItem;
import com.morphengine.nexus.item.CellKind;
import com.morphengine.nexus.item.CellTier;
import com.morphengine.nexus.item.VaultCellItem;
import com.morphengine.nexus.level.AutocraftingComponent;
import com.morphengine.nexus.level.NetworkComponentTypes;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.NetworkStorage;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Autocrafting in a world: Assemblers crafting and handing work to a machine,
 * with the items encoded or their substitutes, a Pusher ordering what its
 * network lacks, and the Blueprint encoder.
 *
 * <p>Every test starts from the same network along the north edge: a Nexus, a
 * charged Energy Cell south of it, a Storage Vault with a cell east of it and a
 * cable east of the vault, the Assembler on top of the cable.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class AutocraftingGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 300;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos VAULT = NEXUS.east();
    private static final BlockPos CABLE = VAULT.east();
    private static final BlockPos ASSEMBLER = CABLE.above();
    private static final BlockPos MACHINE = ASSEMBLER.east();

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("assembler_crafts_sticks_from_planks", AutocraftingGameTests::assemblerCraftsSticks),
            Map.entry("assembler_processes_in_machine_and_collects", AutocraftingGameTests::assemblerProcesses),
            Map.entry("pusher_orders_what_network_lacks", AutocraftingGameTests::pusherOrdersWhatNetworkLacks),
            Map.entry("encoder_writes_crafting_recipe", AutocraftingGameTests::encoderWritesCraftingRecipe),
            Map.entry("assembler_crafts_with_substitute_planks",
                    AutocraftingGameTests::assemblerCraftsWithSubstitutes),
            Map.entry("processing_takes_members_of_input_tag",
                    AutocraftingGameTests::processingTakesTagMembers),
            Map.entry("broken_assembler_gives_back_what_its_tasks_held",
                    AutocraftingGameTests::brokenAssemblerGivesBack));

    private AutocraftingGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "autocrafting"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void assemblerCraftsSticks(final GameTestHelper helper) {
        buildNetwork(helper, Direction.UP, stored(Items.OAK_PLANKS, 2));
        assembler(helper).blueprintSlots().setItem(0, blueprintOf(sticks()));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        autocrafting(helper).blueprints().craftables().contains(key(Items.STICK)),
                        Component.literal("the network does not know how to craft sticks")))
                .thenExecute(() -> start(helper, Items.STICK, 4))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STICK)), 4,
                        "sticks in the network"))
                .thenWaitUntil(() -> helper.assertTrue(autocrafting(helper).statuses().isEmpty(),
                        Component.literal("the task is still kept")))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.OAK_PLANKS)), 0,
                        "planks left in the network"))
                .thenSucceed();
    }

    private static void assemblerProcesses(final GameTestHelper helper) {
        buildNetwork(helper, Direction.EAST, stored(Items.COBBLESTONE, 2));
        place(helper, MACHINE, Blocks.CHEST.defaultBlockState());
        assembler(helper).blueprintSlots().setItem(0, blueprintOf(ProcessingBlueprint.exact(
                List.of(new ResourceAmount(key(Items.COBBLESTONE), 1)),
                List.of(new ResourceAmount(key(Items.STONE), 1)))));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        autocrafting(helper).blueprints().craftables().contains(key(Items.STONE)),
                        Component.literal("the network does not know how to make stone")))
                .thenExecute(() -> start(helper, Items.STONE, 2))
                .thenWaitUntil(() -> assertAmount(helper, count(container(helper, MACHINE), Items.COBBLESTONE), 2,
                        "cobblestone handed to the machine"))
                .thenExecute(() -> {
                    container(helper, MACHINE).clearContent();
                    container(helper, MACHINE).setItem(0, new ItemStack(Items.STONE, 2));
                })
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STONE)), 2,
                        "stone taken back into the network"))
                .thenWaitUntil(() -> helper.assertTrue(autocrafting(helper).statuses().isEmpty(),
                        Component.literal("the task is still kept")))
                .thenSucceed();
    }

    private static void pusherOrdersWhatNetworkLacks(final GameTestHelper helper) {
        buildNetwork(helper, Direction.UP, stored(Items.OAK_PLANKS, 2));
        assembler(helper).blueprintSlots().setItem(0, blueprintOf(sticks()));
        final BlockPos pusher = CABLE.south();
        final BlockPos chest = pusher.south();
        place(helper, chest, Blocks.CHEST.defaultBlockState());
        place(helper, pusher, NexusBlocks.PUSHER.get().defaultBlockState()
                .setValue(TransferDeviceBlock.FACING, Direction.SOUTH));
        final TransferDeviceBlockEntity device = helper.getBlockEntity(pusher, TransferDeviceBlockEntity.class);
        device.changeSettings(device.settings().withFilter(FilterSlots.EMPTY.with(0, key(Items.STICK))));
        device.upgrades().setItem(0, new ItemStack(NexusItems.AUTOCRAFTING_UPGRADE.get()));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(count(container(helper, chest), Items.STICK) > 0,
                        Component.literal("the Pusher delivered no crafted sticks")))
                .thenSucceed();
    }

    private static void encoderWritesCraftingRecipe(final GameTestHelper helper) {
        final BlueprintEncoder encoder = new BlueprintEncoder(() -> { });
        encoder.blueprints().setItem(BlueprintEncoder.BLANK_SLOT, new ItemStack(NexusItems.BLUEPRINT.get(), 3));
        encoder.changeDraft(BlueprintDraft.EMPTY.with(0, key(Items.OAK_PLANKS)).with(3, key(Items.OAK_PLANKS)));

        final boolean encoded = encoder.encode(helper.getLevel());

        final EncodedBlueprint written = BlueprintItem.encodedOn(encoder.blueprints()
                .getItem(BlueprintEncoder.OUTPUT_SLOT));
        helper.assertTrue(encoded && written != null && written.kind() == BlueprintKind.CRAFTING,
                Component.literal("no crafting blueprint came out of the encoder"));
        helper.assertTrue(written.blueprint().primaryOutput().equals(new ResourceAmount(key(Items.STICK), 4)),
                Component.literal("the blueprint gives " + written.blueprint().primaryOutput()));
        assertAmount(helper, encoder.blueprints().getItem(BlueprintEncoder.BLANK_SLOT).getCount(), 2,
                "blank Blueprints left");
        helper.succeed();
    }

    private static void assemblerCraftsWithSubstitutes(final GameTestHelper helper) {
        buildNetwork(helper, Direction.UP, stored(Items.SPRUCE_PLANKS, 2));
        final BlueprintEncoder encoder = new BlueprintEncoder(() -> { });
        encoder.blueprints().setItem(BlueprintEncoder.BLANK_SLOT, new ItemStack(NexusItems.BLUEPRINT.get()));
        encoder.changeDraft(BlueprintDraft.EMPTY.withSubstitution(Substitution.ALLOWED)
                .with(0, key(Items.OAK_PLANKS)).with(3, key(Items.OAK_PLANKS)));
        helper.assertTrue(encoder.encode(helper.getLevel()), Component.literal("the encoder wrote nothing"));
        assembler(helper).blueprintSlots().setItem(0, encoder.blueprints().getItem(BlueprintEncoder.OUTPUT_SLOT));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        autocrafting(helper).blueprints().craftables().contains(key(Items.STICK)),
                        Component.literal("the network does not know how to craft sticks")))
                .thenExecute(() -> start(helper, Items.STICK, 4))
                .thenWaitUntil(() -> assertAmount(helper, network(helper).amountOf(key(Items.STICK)), 4,
                        "sticks crafted from spruce planks"))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.SPRUCE_PLANKS)), 0,
                        "spruce planks left in the network"))
                .thenSucceed();
    }

    private static void processingTakesTagMembers(final GameTestHelper helper) {
        buildNetwork(helper, Direction.EAST, stored(Items.BIRCH_PLANKS, 1));
        place(helper, MACHINE, Blocks.CHEST.defaultBlockState());
        final ProcessingInput anyPlanks = new ProcessingInput(new ResourceAmount(key(Items.OAK_PLANKS), 1),
                ItemTags.PLANKS.location());
        assembler(helper).blueprintSlots().setItem(0, blueprintOf(new ProcessingBlueprint(List.of(anyPlanks),
                List.of(new ResourceAmount(key(Items.CHARCOAL), 1)), Substitution.ALLOWED)));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        autocrafting(helper).blueprints().craftables().contains(key(Items.CHARCOAL)),
                        Component.literal("the network does not know how to make charcoal")))
                .thenExecute(() -> start(helper, Items.CHARCOAL, 1))
                .thenWaitUntil(() -> assertAmount(helper, count(container(helper, MACHINE), Items.BIRCH_PLANKS), 1,
                        "birch planks handed to the machine for oak planks"))
                .thenSucceed();
    }

    private static void brokenAssemblerGivesBack(final GameTestHelper helper) {
        buildNetwork(helper, Direction.EAST, stored(Items.COBBLESTONE, 3));
        place(helper, MACHINE, Blocks.CHEST.defaultBlockState());
        assembler(helper).changeSettings(assembler(helper).settings().withLock(LockMode.UNTIL_EMPTY));
        assembler(helper).blueprintSlots().setItem(0, blueprintOf(ProcessingBlueprint.exact(
                List.of(new ResourceAmount(key(Items.COBBLESTONE), 1)),
                List.of(new ResourceAmount(key(Items.STONE), 1)))));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        autocrafting(helper).blueprints().craftables().contains(key(Items.STONE)),
                        Component.literal("the network does not know how to make stone")))
                .thenExecute(() -> start(helper, Items.STONE, 3))
                .thenWaitUntil(() -> assertAmount(helper, count(container(helper, MACHINE), Items.COBBLESTONE), 1,
                        "cobblestone handed to the locked machine"))
                .thenExecute(() -> helper.destroyBlock(ASSEMBLER))
                .thenExecute(() -> assertAmount(helper, network(helper).amountOf(key(Items.COBBLESTONE)), 2,
                        "cobblestone given back to the network"))
                .thenSucceed();
    }

    /**
     * @param contents what the vault's cell holds from the start
     */
    private static void buildNetwork(final GameTestHelper helper, final Direction assemblerFacing,
                                     final ResourceAmount contents) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        charge(helper, CELL);
        place(helper, VAULT, NexusBlocks.STORAGE_VAULT.get().defaultBlockState()
                .setValue(StorageVaultBlock.FACING, Direction.SOUTH));
        final ItemStack cell = new ItemStack(NexusItems.VAULT_CELLS.get(CellKind.ITEM).get(CellTier.ONE_K).get());
        VaultCellItem.saveContents(cell, List.of(contents));
        helper.getBlockEntity(VAULT, StorageVaultBlockEntity.class).cells().setItem(0, cell);
        place(helper, CABLE, NexusBlocks.CABLES.get(DyeColor.BLUE).get().defaultBlockState());
        place(helper, ASSEMBLER, NexusBlocks.ASSEMBLER.get().defaultBlockState()
                .setValue(AssemblerBlock.FACING, assemblerFacing));
    }

    private static void charge(final GameTestHelper helper, final BlockPos pos) {
        final EnergyHandler handler = helper.getLevel()
                .getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.WEST);
        helper.assertTrue(handler != null, Component.literal("no energy cell at " + pos));
        try (Transaction transaction = Transaction.openRoot()) {
            handler.insert(Integer.MAX_VALUE, transaction);
            transaction.commit();
        }
    }

    private static ResourceAmount stored(final Item item, final long count) {
        return new ResourceAmount(key(item), count);
    }

    private static void start(final GameTestHelper helper, final Item item, final long amount) {
        final CraftingPlan plan = autocrafting(helper).plan(key(item), amount, network(helper));
        helper.assertTrue(autocrafting(helper).start(plan, "test"),
                Component.literal("the plan did not start, missing " + plan.missing()));
    }

    private static CraftingBlueprint sticks() {
        return new CraftingBlueprint(
                List.of(new GridSlot(0, key(Items.OAK_PLANKS)), new GridSlot(3, key(Items.OAK_PLANKS))),
                List.of(new ResourceAmount(key(Items.STICK), 4)));
    }

    private static ItemStack blueprintOf(final EncodedBlueprint blueprint) {
        final ItemStack stack = new ItemStack(NexusItems.BLUEPRINT.get());
        stack.set(NexusDataComponents.ENCODED_BLUEPRINT.get(), blueprint);
        return stack;
    }

    private static AssemblerBlockEntity assembler(final GameTestHelper helper) {
        return helper.getBlockEntity(ASSEMBLER, AssemblerBlockEntity.class);
    }

    private static AutocraftingComponent autocrafting(final GameTestHelper helper) {
        return helper.getBlockEntity(NEXUS, NexusBlockEntity.class).component(NetworkComponentTypes.AUTOCRAFTING);
    }

    private static NetworkStorage network(final GameTestHelper helper) {
        return helper.getBlockEntity(NEXUS, NexusBlockEntity.class)
                .component(NetworkComponentTypes.STORAGE).storage();
    }

    private static Container container(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
    }

    private static int count(final Container container, final Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) {
                total += container.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static void assertAmount(final GameTestHelper helper, final long actual, final long expected,
                                     final String what) {
        helper.assertTrue(actual == expected, Component.literal(what + ": " + actual + ", expected " + expected));
    }

    private static ItemKey key(final Item item) {
        return ItemKey.of(new ItemStack(item));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
