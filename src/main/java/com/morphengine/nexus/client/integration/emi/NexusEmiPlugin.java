package com.morphengine.nexus.client.integration.emi;

import com.morphengine.nexus.Nexus;
import com.morphengine.nexus.client.screen.SideAreas;
import com.morphengine.nexus.client.screen.TerminalScreen;
import com.morphengine.nexus.processing.AlloyingRecipe;
import com.morphengine.nexus.processing.MachineKind;
import com.morphengine.nexus.processing.ProcessingRecipe;
import com.morphengine.nexus.registry.NexusItems;
import com.morphengine.nexus.registry.NexusMenuTypes;
import com.morphengine.nexus.registry.NexusRecipes;
import com.morphengine.nexus.resource.FluidKey;
import com.morphengine.nexus.resource.ItemKey;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;

/**
 * EMI support: it lists the recipes of the machines, each in a category of its own with the machines of every tier as
 * its workstations, and shows the Energy Furnace as a workstation of the furnaces and the Crafting Terminal as one of
 * the crafting table. Its "+" button lays out a crafting recipe on the grid of a Crafting Terminal and any recipe as
 * the draft of a Blueprint Terminal, it keeps its overlay off the buttons and windows beside the screens that have
 * them, its recipe and usage keys work on the resources a terminal lists, and its items and fluids can be dragged onto
 * the filter of any panel with one.
 */
@EmiEntrypoint
public final class NexusEmiPlugin implements EmiPlugin {

    @Override
    public void register(final EmiRegistry registry) {
        final EmiRecipeCategory crushing = category(registry, "crushing", MachineKind.CRUSHER);
        final EmiRecipeCategory pulverizing = category(registry, "pulverizing", MachineKind.PULVERIZER);
        final EmiRecipeCategory compressing = category(registry, "compressing", MachineKind.COMPRESSOR);
        final EmiRecipeCategory alloying = category(registry, "alloying", MachineKind.ALLOY_SMELTER);
        final EmiRecipeCategory extracting = category(registry, "extracting", MachineKind.EXTRACTOR);
        addItemRecipes(registry, crushing, NexusRecipes.CRUSHING.get());
        addItemRecipes(registry, pulverizing, NexusRecipes.PULVERIZING.get());
        addItemRecipes(registry, compressing, NexusRecipes.COMPRESSING.get());
        addAlloyingRecipes(registry, alloying);
        addExtractingRecipes(registry, extracting);
        for (var item : NexusItems.MACHINES.get(MachineKind.ENERGY_FURNACE)) {
            registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(item.get()));
            registry.addWorkstation(VanillaEmiRecipeCategories.BLASTING, EmiStack.of(item.get()));
            registry.addWorkstation(VanillaEmiRecipeCategories.SMOKING, EmiStack.of(item.get()));
        }
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(NexusItems.CRAFTING_TERMINAL.get()));
        registerHandlers(registry);
        registerScreens(registry);
    }

    private static EmiRecipeCategory category(final EmiRegistry registry, final String name, final MachineKind kind) {
        final ItemLike first = NexusItems.MACHINES.get(kind).getFirst().get();
        final EmiRecipeCategory category = new MachineCategory(
                ResourceLocation.fromNamespaceAndPath(Nexus.MOD_ID, name), "gui.nexus.recipes." + name, first);
        registry.addCategory(category);
        for (var machine : NexusItems.MACHINES.get(kind)) {
            registry.addWorkstation(category, EmiStack.of(machine.get()));
        }
        return category;
    }

    private static <R extends ProcessingRecipe> void addItemRecipes(
            final EmiRegistry registry, final EmiRecipeCategory category, final RecipeType<R> type) {
        for (RecipeHolder<R> holder : registry.getRecipeManager().getAllRecipesFor(type)) {
            final R recipe = holder.value();
            registry.addRecipe(new MachineEmiRecipe(category, holder.id(),
                    List.of(EmiIngredient.of(recipe.ingredient())), EmiStack.of(recipe.result())));
        }
    }

    private static void addAlloyingRecipes(final EmiRegistry registry, final EmiRecipeCategory category) {
        for (RecipeHolder<AlloyingRecipe> holder
                : registry.getRecipeManager().getAllRecipesFor(NexusRecipes.ALLOYING.get())) {
            final AlloyingRecipe recipe = holder.value();
            final List<EmiIngredient> inputs = recipe.ingredients().stream()
                    .map(needed -> (EmiIngredient) EmiIngredient.of(needed.ingredient(), needed.count())).toList();
            registry.addRecipe(new MachineEmiRecipe(category, holder.id(), inputs, EmiStack.of(recipe.resultStack())));
        }
    }

    private static void addExtractingRecipes(final EmiRegistry registry, final EmiRecipeCategory category) {
        for (var holder : registry.getRecipeManager().getAllRecipesFor(NexusRecipes.EXTRACTING.get())) {
            final var recipe = holder.value();
            registry.addRecipe(new MachineEmiRecipe(category, holder.id(),
                    List.of(EmiIngredient.of(recipe.ingredient())), EmiStack.of(recipe.fluid(), recipe.amount())));
        }
    }

    private static void registerHandlers(final EmiRegistry registry) {
        registry.addRecipeHandler(NexusMenuTypes.CRAFTING_TERMINAL.get(), new CraftingTerminalHandler());
        registry.addRecipeHandler(NexusMenuTypes.PORTABLE_CRAFTING_TERMINAL.get(), new CraftingTerminalHandler());
        registry.addRecipeHandler(NexusMenuTypes.BLUEPRINT_TERMINAL.get(), new BlueprintTerminalHandler());
        registry.addRecipeHandler(NexusMenuTypes.PORTABLE_BLUEPRINT_TERMINAL.get(), new BlueprintTerminalHandler());
    }

    private static void registerScreens(final EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof SideAreas areas) {
                for (Rect2i area : areas.extraAreas()) {
                    consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });
        registry.addGenericStackProvider((screen, mouseX, mouseY) -> stackAt(screen, mouseX, mouseY));
        registry.addGenericDragDropHandler(new FilterDropHandler());
    }

    private static EmiStackInteraction stackAt(final Screen screen, final int mouseX, final int mouseY) {
        if (!(screen instanceof TerminalScreen<?> terminal)) {
            return EmiStackInteraction.EMPTY;
        }
        final TerminalScreen.HoveredResource hovered = terminal.hoveredResource(mouseX, mouseY);
        if (hovered == null) {
            return EmiStackInteraction.EMPTY;
        }
        return switch (hovered.resource()) {
            case ItemKey item -> unclickable(EmiStack.of(item.toStack(1)));
            case FluidKey fluid -> unclickable(EmiStack.of(fluid.fluid().getFluid(),
                    FluidType.BUCKET_VOLUME));
            default -> EmiStackInteraction.EMPTY;
        };
    }

    /**
     * A stack EMI can show recipes for from its keys but that a click in the terminal does not open: clicks there take
     * the resource or ask for it to be crafted.
     */
    private static EmiStackInteraction unclickable(final EmiStack stack) {
        return new EmiStackInteraction(stack, null, false);
    }
}
