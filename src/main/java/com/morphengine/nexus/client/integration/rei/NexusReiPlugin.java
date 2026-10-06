package com.morphengine.nexus.client.integration.rei;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.integration.MachineRecipes;
import com.morphengine.nexus.client.screen.AssemblerScreen;
import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.client.screen.TransferDeviceScreen;
import com.morphengine.nexus.processing.AlloyIngredient;
import com.morphengine.nexus.processing.AlloyingRecipe;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.registry.NexusRecipes;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

/**
 * REI support: its "+" button lays out a crafting recipe on the grid of a
 * Crafting Terminal and any recipe as the draft of a Blueprint Terminal, it
 * keeps its overlay off the side buttons of terminals, Pullers, Pushers and
 * Assemblers, its recipe and usage keys work on the resources a terminal
 * lists, and its items and fluids can be dragged onto the filter of any panel
 * with one and onto the Blueprint encoder. It lists the recipes of the machines, each in a category of its own with
 * the machines of every tier as its workstations, and shows the Energy Furnace as a workstation of the furnaces and the
 * Crafting Terminal as one of the crafting table.
 */
@REIPluginClient
public final class NexusReiPlugin implements REIClientPlugin {

    private static final CategoryIdentifier<MachineDisplay> CRUSHING = machineCategory("crushing");
    private static final CategoryIdentifier<MachineDisplay> PULVERIZING = machineCategory("pulverizing");
    private static final CategoryIdentifier<MachineDisplay> COMPRESSING = machineCategory("compressing");
    private static final CategoryIdentifier<MachineDisplay> ALLOYING = machineCategory("alloying");
    private static final CategoryIdentifier<MachineDisplay> EXTRACTING = machineCategory("extracting");
    private static final CategoryIdentifier<?> CRAFTING = CategoryIdentifier.of("minecraft", "plugins/crafting");
    private static final CategoryIdentifier<?> SMELTING = CategoryIdentifier.of("minecraft", "plugins/smelting");
    private static final CategoryIdentifier<?> BLASTING = CategoryIdentifier.of("minecraft", "plugins/blasting");
    private static final CategoryIdentifier<?> SMOKING = CategoryIdentifier.of("minecraft", "plugins/smoking");

    private static CategoryIdentifier<MachineDisplay> machineCategory(final String name) {
        return CategoryIdentifier.of(Nexus.MOD_ID, name);
    }

    @Override
    public void registerCategories(final CategoryRegistry registry) {
        addMachineCategory(registry, CRUSHING, MachineKind.CRUSHER);
        addMachineCategory(registry, PULVERIZING, MachineKind.PULVERIZER);
        addMachineCategory(registry, COMPRESSING, MachineKind.COMPRESSOR);
        addMachineCategory(registry, ALLOYING, MachineKind.ALLOY_SMELTER);
        addMachineCategory(registry, EXTRACTING, MachineKind.EXTRACTOR);
        final EntryStack<?>[] furnaces = machines(MachineKind.ENERGY_FURNACE);
        registry.addWorkstations(SMELTING, furnaces);
        registry.addWorkstations(BLASTING, furnaces);
        registry.addWorkstations(SMOKING, furnaces);
        registry.addWorkstations(CRAFTING, EntryStacks.of(NexusItems.CRAFTING_TERMINAL.get()));
    }

    private static void addMachineCategory(
            final CategoryRegistry registry, final CategoryIdentifier<MachineDisplay> id, final MachineKind kind) {
        registry.add(new MachineCategory(id, "gui.nexus.recipes." + id.getPath(),
                NexusItems.MACHINES.get(kind).getFirst().get()));
        registry.addWorkstations(id, machines(kind));
    }

    private static EntryStack<?>[] machines(final MachineKind kind) {
        return NexusItems.MACHINES.get(kind).stream().map(item -> EntryStacks.of(item.get()))
                .toArray(EntryStack<?>[]::new);
    }

    @Override
    public void registerDisplays(final DisplayRegistry registry) {
        addItemRecipes(registry, CRUSHING, NexusRecipes.CRUSHING.get());
        addItemRecipes(registry, PULVERIZING, NexusRecipes.PULVERIZING.get());
        addItemRecipes(registry, COMPRESSING, NexusRecipes.COMPRESSING.get());
        for (RecipeHolder<AlloyingRecipe> holder : MachineRecipes.of(NexusRecipes.ALLOYING.get())) {
            final AlloyingRecipe recipe = holder.value();
            registry.add(new MachineDisplay(ALLOYING, recipe.ingredients().stream().map(NexusReiPlugin::entriesOf)
                    .toList(), List.of(EntryIngredients.of(recipe.resultStack())), holder.id().identifier()));
        }
        for (var holder : MachineRecipes.of(NexusRecipes.EXTRACTING.get())) {
            final var recipe = holder.value();
            registry.add(new MachineDisplay(EXTRACTING, List.of(EntryIngredients.ofIngredient(recipe.ingredient())),
                    List.of(EntryIngredients.of(recipe.fluid(), recipe.amount())), holder.id().identifier()));
        }
    }

    private static <R extends SingleItemRecipe> void addItemRecipes(
            final DisplayRegistry registry, final CategoryIdentifier<MachineDisplay> id, final RecipeType<R> type) {
        for (RecipeHolder<R> holder : MachineRecipes.of(type)) {
            final R recipe = holder.value();
            final ItemStack result = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
            registry.add(new MachineDisplay(id, List.of(EntryIngredients.ofIngredient(recipe.input())),
                    List.of(EntryIngredients.of(result)), holder.id().identifier()));
        }
    }

    private static EntryIngredient entriesOf(final AlloyIngredient needed) {
        return EntryIngredients.ofItemStacks(needed.ingredient().items()
                .map(item -> new ItemStack(item, needed.count())).toList());
    }

    @Override
    public void registerTransferHandlers(final TransferHandlerRegistry registry) {
        registry.register(new CraftingTerminalTransferHandler());
        registry.register(new BlueprintTerminalTransferHandler());
    }

    @Override
    public void registerExclusionZones(final ExclusionZones zones) {
        zones.register(TerminalScreen.class, (TerminalScreen<?> screen) -> List.of(rectangle(screen.sidebarArea())));
        zones.register(TransferDeviceScreen.class,
                (TransferDeviceScreen screen) -> List.of(rectangle(screen.sidebarArea())));
        zones.register(AssemblerScreen.class, (AssemblerScreen screen) -> List.of(rectangle(screen.sidebarArea())));
    }

    private static Rectangle rectangle(final Rect2i area) {
        return new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    @Override
    public void registerScreens(final ScreenRegistry registry) {
        registry.registerFocusedStack(new TerminalFocusedStack());
        registry.registerDraggableStackVisitor(new FilterDragVisitor());
    }
}
