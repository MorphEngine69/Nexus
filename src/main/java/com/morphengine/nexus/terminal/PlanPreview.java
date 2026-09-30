package com.morphengine.nexus.terminal;

import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Objects;

/**
 * A crafting plan as the player previews it before starting: what it takes
 * from the network, what it crafts and what is missing.
 */
public record PlanPreview(ResourceAmount target, List<ResourceAmount> fromStorage, List<ResourceAmount> crafted,
                          List<ResourceAmount> missing) {

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanPreview> STREAM_CODEC = StreamCodec.composite(
            NexusResources.AMOUNT_STREAM_CODEC, PlanPreview::target,
            amounts(), PlanPreview::fromStorage,
            amounts(), PlanPreview::crafted,
            amounts(), PlanPreview::missing,
            PlanPreview::new);

    public PlanPreview {
        Objects.requireNonNull(target, "target must not be null");
        fromStorage = List.copyOf(fromStorage);
        crafted = List.copyOf(crafted);
        missing = List.copyOf(missing);
    }

    public static PlanPreview of(final CraftingPlan plan) {
        return new PlanPreview(plan.target(), plan.fromStorage(), plan.crafted(), plan.missing());
    }

    private static StreamCodec<RegistryFriendlyByteBuf, List<ResourceAmount>> amounts() {
        return NexusResources.AMOUNT_STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    /**
     * @return whether the plan can start: nothing is missing and something gets crafted
     */
    public boolean isComplete() {
        return missing.isEmpty() && !crafted.isEmpty();
    }
}
