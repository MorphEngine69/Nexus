package com.morphengine.nexus.blueprint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.resource.NexusResources;
import com.morphengine.nexus.storage.ResourceCounter;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A crafting recipe laid out on a 3 by 3 grid, one item per filled slot, and
 * what crafting it gave when it was encoded: the result first, then what the
 * ingredients leave behind, such as an empty bucket.
 *
 * @param grid         the filled slots, each once
 * @param outputs      what one craft gives
 * @param substitution whether a slot also takes what else the recipe accepts in it
 */
public record CraftingBlueprint(List<GridSlot> grid, List<ResourceAmount> outputs, Substitution substitution)
        implements EncodedBlueprint {

    public static final MapCodec<CraftingBlueprint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    GridSlot.CODEC.listOf().fieldOf("grid").forGetter(CraftingBlueprint::grid),
                    NexusResources.AMOUNT_CODEC.listOf().fieldOf("outputs").forGetter(CraftingBlueprint::outputs),
                    Substitution.CODEC.optionalFieldOf("substitution", Substitution.EXACT)
                            .forGetter(CraftingBlueprint::substitution))
            .apply(instance, CraftingBlueprint::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingBlueprint> STREAM_CODEC =
            StreamCodec.composite(
                    GridSlot.STREAM_CODEC.apply(ByteBufCodecs.list(GridSlot.COUNT)), CraftingBlueprint::grid,
                    NexusResources.AMOUNT_STREAM_CODEC.apply(ByteBufCodecs.list()), CraftingBlueprint::outputs,
                    Substitution.STREAM_CODEC, CraftingBlueprint::substitution,
                    CraftingBlueprint::new);

    public CraftingBlueprint {
        grid = List.copyOf(grid);
        outputs = List.copyOf(outputs);
        Objects.requireNonNull(substitution, "substitution must not be null");
        final Set<Integer> filled = new HashSet<>();
        for (GridSlot slot : grid) {
            if (!filled.add(slot.slot())) {
                throw new IllegalArgumentException("grid slot " + slot.slot() + " filled twice: " + grid);
            }
        }
        if (grid.isEmpty() || outputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "crafting blueprint needs ingredients and outputs: grid=" + grid + ", outputs=" + outputs);
        }
    }

    /**
     * A blueprint whose slots take nothing but the items encoded.
     */
    public CraftingBlueprint(final List<GridSlot> grid, final List<ResourceAmount> outputs) {
        this(grid, outputs, Substitution.EXACT);
    }

    @Override
    public BlueprintKind kind() {
        return BlueprintKind.CRAFTING;
    }

    @Override
    public Blueprint blueprint() {
        final List<BlueprintInput> inputs = new ArrayList<>(grid.size());
        for (GridSlot slot : grid) {
            inputs.add(new BlueprintInput(slot.options(substitution), 1));
        }
        return new Blueprint(BlueprintKind.CRAFTING, inputs, outputs);
    }

    /**
     * @return one stack per slot of the grid, row by row, with the items
     *         encoded; empty where nothing lies
     */
    public List<ItemStack> stacks() {
        final NonNullList<ItemStack> stacks = NonNullList.withSize(GridSlot.COUNT, ItemStack.EMPTY);
        for (GridSlot slot : grid) {
            stacks.set(slot.slot(), slot.item().toStack(1));
        }
        return stacks;
    }

    /**
     * Lays the items picked for one run out on the grid, each slot getting one
     * item it takes; slots taking fewer items are filled first.
     *
     * @param picked the items picked for one run, as the network hands them over
     * @return one stack per slot of the grid, row by row; empty when the picked
     *         items do not fill every slot
     */
    public List<ItemStack> stacksOf(final List<ResourceAmount> picked) {
        final ResourceCounter left = new ResourceCounter();
        for (ResourceAmount amount : picked) {
            left.add(amount.resource(), amount.amount());
        }
        final List<SlotOptions> order = new ArrayList<>(grid.size());
        for (GridSlot slot : grid) {
            order.add(new SlotOptions(slot.slot(), slot.options(substitution)));
        }
        order.sort(Comparator.comparingInt(slot -> slot.options().size()));
        final NonNullList<ItemStack> stacks = NonNullList.withSize(GridSlot.COUNT, ItemStack.EMPTY);
        for (SlotOptions slot : order) {
            final ItemKey item = firstHeld(slot.options(), left);
            if (item == null) {
                return List.of();
            }
            left.remove(item, 1);
            stacks.set(slot.index(), item.toStack(1));
        }
        return stacks;
    }

    private static @Nullable ItemKey firstHeld(final List<ResourceKey> options,
                                                                        final ResourceCounter held) {
        for (ResourceKey option : options) {
            if (held.amountOf(option) > 0 && option instanceof ItemKey item) {
                return item;
            }
        }
        return null;
    }

    /**
     * A slot of the grid with the items it takes, filled in the order the
     * network picks inputs: those taking fewer items first.
     */
    private record SlotOptions(int index, List<ResourceKey> options) {
    }
}
