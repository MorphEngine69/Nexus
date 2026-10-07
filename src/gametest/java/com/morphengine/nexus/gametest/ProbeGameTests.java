package com.morphengine.nexus.gametest;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.block.MachineBlock;
import com.morphengine.nexus.block.entity.GeneratorBlockEntity;
import com.morphengine.nexus.block.entity.MachineBlockEntity;
import com.morphengine.nexus.block.entity.NexusBlockEntity;
import com.morphengine.nexus.block.entity.TransferDeviceBlockEntity;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.generator.GeneratorKind;
import com.morphengine.nexus.item.StoredFluids;
import com.morphengine.nexus.probe.BlockProbes;
import com.morphengine.nexus.probe.ProbeLine;
import com.morphengine.nexus.probe.ProbeReport;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.registry.NexusBlocks;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.resource.ItemKey;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * What a tooltip mod is told about the blocks of Nexus: a machine, a generator and a Nexus in a network, and a device
 * that is not in one. The report is the same whichever mod draws it.
 */
@EventBusSubscriber(modid = Nexus.MOD_ID)
public final class ProbeGameTests {

    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "platform");
    private static final int MAX_TICKS = 200;
    private static final BlockPos NEXUS = new BlockPos(1, 1, 1);
    private static final BlockPos CELL = NEXUS.south();
    private static final BlockPos DEVICE = NEXUS.east();
    private static final BlockPos LONELY = new BlockPos(6, 1, 6);
    private static final long STORED_FE = 4_321;
    private static final int STORED_FLUID = 2_500;

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "a_placer_report_says_what_it_places_and_what_it_is_set_to", ProbeGameTests::placerReport,
            "a_remover_without_a_filter_says_it_takes_anything", ProbeGameTests::removerReport,
            "a_fluid_generator_shows_no_burn_progress", ProbeGameTests::fluidGeneratorHasNoBurnBar,
            "a_machine_report_has_its_energy_and_its_network", ProbeGameTests::machineReport,
            "a_generator_report_has_its_tanks", ProbeGameTests::generatorReport,
            "a_nexus_report_has_the_energy_of_its_network", ProbeGameTests::nexusReport,
            "a_machine_outside_a_network_says_it_works_alone", ProbeGameTests::machineOutsideNetwork,
            "a_generator_outside_a_network_says_it_works_alone", ProbeGameTests::generatorOutsideNetwork,
            "a_device_that_needs_a_network_says_it_has_none", ProbeGameTests::deviceOutsideNetwork,
            "a_report_survives_the_wire", ProbeGameTests::reportSurvivesWire);

    private ProbeGameTests() {
    }

    @SubscribeEvent
    static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(
                (name, test) -> helper.register(Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name), test)));
    }

    @SubscribeEvent
    static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nexus.MOD_ID, "probe"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            final Identifier id = Identifier.fromNamespaceAndPath(Nexus.MOD_ID, name);
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true)));
        }
    }

    private static void placerReport(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.PLACER.get().defaultBlockState());
        final TransferDeviceBlockEntity placer = helper.getBlockEntity(LONELY, TransferDeviceBlockEntity.class);
        placer.changeSettings(placer.settings()
                .withFilter(FilterSlots.EMPTY.with(0, ItemKey.of(new ItemStack(Items.STONE)))));

        final ProbeReport report = reportOf(helper, LONELY);

        helper.assertTrue(hasText(report, "tooltip.nexus.probe.act.placer.blocks"),
                Component.literal("a Placer does not say it places blocks: " + report.lines()));
        helper.assertTrue(hasText(report, "tooltip.nexus.probe.filter.only"),
                Component.literal("a Placer does not name what it places: " + report.lines()));
        helper.assertFalse(hasText(report, "gui.nexus.transfer.resource"),
                Component.literal("a Placer still says what it transfers: " + report.lines()));
        helper.succeed();
    }

    private static void removerReport(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.REMOVER.get().defaultBlockState());

        final ProbeReport report = reportOf(helper, LONELY);

        helper.assertTrue(hasText(report, "tooltip.nexus.probe.act.remover.blocks"),
                Component.literal("a Remover does not say it breaks blocks: " + report.lines()));
        helper.assertTrue(hasText(report, "tooltip.nexus.probe.filter.anything"),
                Component.literal("a Remover without a filter does not say it takes anything: " + report.lines()));
        helper.succeed();
    }

    private static void fluidGeneratorHasNoBurnBar(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.GENERATORS.get(GeneratorKind.BIOFUEL).get().defaultBlockState());

        final ProbeReport report = reportOf(helper, LONELY);

        helper.assertFalse(report.lines().stream().anyMatch(ProbeLine.Progress.class::isInstance),
                Component.literal("a generator of fluid shows a burn bar: " + report.lines()));
        helper.succeed();
    }

    private static void machineReport(final GameTestHelper helper) {
        buildNetwork(helper);
        place(helper, DEVICE, NexusBlocks.machineTiers(MachineKind.CRUSHER).getFirst().get().defaultBlockState()
                .setValue(MachineBlock.FACING, Direction.NORTH));
        seed(helper.getBlockEntity(DEVICE, MachineBlockEntity.class), STORED_FE, StoredFluids.EMPTY);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        helper.getBlockEntity(DEVICE, MachineBlockEntity.class).networkBadge() != null,
                        Component.literal("the machine is not in the network")))
                .thenExecute(() -> {
                    final ProbeReport report = reportOf(helper, DEVICE);
                    helper.assertTrue(report.lines().stream().anyMatch(line -> line instanceof ProbeLine.Energy energy
                                    && energy.stored() >= STORED_FE && energy.capacity() == capacityOf(helper)),
                            Component.literal("no energy bar with what the machine holds: " + report.lines()));
                    helper.assertTrue(hasText(report, "gui.nexus.network"),
                            Component.literal("the network is not named: " + report.lines()));
                    helper.assertFalse(hasText(report, "gui.nexus.terminal.no_network"),
                            Component.literal("a machine in a network says it has none"));
                })
                .thenSucceed();
    }

    private static void generatorReport(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.GENERATORS.get(GeneratorKind.LAVA).get().defaultBlockState());
        seed(helper.getBlockEntity(LONELY, GeneratorBlockEntity.class), STORED_FE,
                new StoredFluids(List.of(new FluidStack(Fluids.LAVA, STORED_FLUID))));

        final ProbeReport report = reportOf(helper, LONELY);

        helper.assertTrue(report.lines().stream().anyMatch(line -> line instanceof ProbeLine.Tank tank
                        && tank.contents().is(Fluids.LAVA) && tank.contents().getAmount() == STORED_FLUID),
                Component.literal("no tank with the lava: " + report.lines()));
        helper.assertTrue(report.lines().stream().anyMatch(line -> line instanceof ProbeLine.Energy energy
                        && energy.stored() == STORED_FE),
                Component.literal("no energy bar: " + report.lines()));
        helper.succeed();
    }

    private static void nexusReport(final GameTestHelper helper) {
        buildNetwork(helper);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        helper.getBlockEntity(NEXUS, NexusBlockEntity.class).statistics().energyCapacity() > 0,
                        Component.literal("the network has not counted its energy")))
                .thenExecute(() -> {
                    final ProbeReport report = reportOf(helper, NEXUS);
                    helper.assertTrue(report.lines().stream().anyMatch(ProbeLine.Energy.class::isInstance),
                            Component.literal("the Nexus shows no energy: " + report.lines()));
                    helper.assertTrue(hasText(report, "gui.nexus.network"),
                            Component.literal("the Nexus does not name its network: " + report.lines()));
                })
                .thenSucceed();
    }

    private static void machineOutsideNetwork(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.machineTiers(MachineKind.CRUSHER).getFirst().get().defaultBlockState());

        assertWorksAlone(helper, reportOf(helper, LONELY), "a machine");
        helper.succeed();
    }

    private static void generatorOutsideNetwork(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.GENERATORS.get(GeneratorKind.COAL).get().defaultBlockState());

        assertWorksAlone(helper, reportOf(helper, LONELY), "a generator");
        helper.succeed();
    }

    private static void deviceOutsideNetwork(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.PULLER.get().defaultBlockState());

        final ProbeReport report = reportOf(helper, LONELY);

        helper.assertTrue(hasText(report, "gui.nexus.terminal.no_network"),
                Component.literal("a Puller alone does not say it has no network: " + report.lines()));
        helper.assertFalse(hasText(report, "gui.nexus.standalone"),
                Component.literal("a Puller alone says it works standalone: " + report.lines()));
        helper.succeed();
    }

    private static void assertWorksAlone(final GameTestHelper helper, final ProbeReport report, final String what) {
        helper.assertTrue(hasText(report, "gui.nexus.standalone"),
                Component.literal(what + " alone does not say it works on its own: " + report.lines()));
        helper.assertFalse(hasText(report, "gui.nexus.terminal.no_network"),
                Component.literal(what + " alone says it is missing a network: " + report.lines()));
    }

    private static void reportSurvivesWire(final GameTestHelper helper) {
        place(helper, LONELY, NexusBlocks.GENERATORS.get(GeneratorKind.LAVA).get().defaultBlockState());
        seed(helper.getBlockEntity(LONELY, GeneratorBlockEntity.class), STORED_FE,
                new StoredFluids(List.of(new FluidStack(Fluids.LAVA, STORED_FLUID))));
        final ProbeReport sent = reportOf(helper, LONELY);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());

        ProbeReport.STREAM_CODEC.encode(buffer, sent);
        final ProbeReport received = ProbeReport.STREAM_CODEC.decode(buffer);

        helper.assertValueEqual(received.lines().size(), sent.lines().size(),
                Component.literal("lines after the wire"));
        helper.assertTrue(received.lines().stream().anyMatch(line -> line instanceof ProbeLine.Tank tank
                        && tank.contents().is(Fluids.LAVA) && tank.contents().getAmount() == STORED_FLUID),
                Component.literal("the tank is gone after the wire: " + received.lines()));
        helper.succeed();
    }

    private static ProbeReport reportOf(final GameTestHelper helper, final BlockPos pos) {
        return BlockProbes.of(helper.getBlockEntity(pos, BlockEntity.class));
    }

    private static boolean hasText(final ProbeReport report, final String key) {
        return report.lines().stream().anyMatch(line -> line instanceof ProbeLine.Text text
                && text.text().getContents() instanceof TranslatableContents content
                && content.getKey().equals(key));
    }

    private static long capacityOf(final GameTestHelper helper) {
        return helper.getBlockEntity(DEVICE, MachineBlockEntity.class).machine().energy().capacity();
    }

    private static void buildNetwork(final GameTestHelper helper) {
        place(helper, NEXUS, NexusBlocks.NEXUS.get().defaultBlockState());
        place(helper, CELL, NexusBlocks.BASIC_ENERGY_CELL.get().defaultBlockState());
        TestEnergy.charge(helper, CELL, 10_000);
    }

    private static void seed(final BlockEntity device, final long energy, final StoredFluids fluids) {
        final ItemStack stack = new ItemStack(Items.STONE);
        stack.set(NexusDataComponents.STORED_ENERGY.get(), energy);
        stack.set(NexusDataComponents.STORED_FLUIDS.get(), fluids);
        device.applyComponentsFromItemStack(stack);
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final BlockState state) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(state, helper.getLevel(), helper.absolutePos(pos)));
    }
}
