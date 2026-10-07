package com.morphengine.nexus.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.automation.TaskEntry;
import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.automation.BlueprintRunSnapshot;
import com.morphengine.nexus.automation.CraftingTaskSnapshot;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.List;
import java.util.Locale;

/**
 * How blueprints and crafting tasks are saved and sent. Enums of the API are
 * saved by their names in lower case.
 */
public final class BlueprintCodecs {

    public static final Codec<BlueprintKind> KIND_CODEC = enumCodec(BlueprintKind.values());

    /**
     * An input with the resources it accepts. An input saved as a plain amount,
     * before inputs took substitutes, reads as one accepting nothing else.
     */
    public static final Codec<BlueprintInput> INPUT_CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                            NexusResources.CODEC.listOf().fieldOf("options")
                                    .forGetter(input -> NexusResources.listOf(input.options())),
                            Codec.LONG.fieldOf("amount").forGetter(BlueprintInput::amount))
                    .apply(instance, (options, amount) -> new BlueprintInput(List.copyOf(options), amount))),
            NexusResources.AMOUNT_CODEC.xmap(BlueprintInput::of, input -> new ResourceAmount(input.preferred(),
                    input.amount())));

    public static final Codec<Blueprint> BLUEPRINT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    KIND_CODEC.fieldOf("kind").forGetter(Blueprint::kind),
                    INPUT_CODEC.listOf().fieldOf("inputs").forGetter(Blueprint::inputs),
                    NexusResources.AMOUNT_CODEC.listOf().fieldOf("outputs").forGetter(Blueprint::outputs))
            .apply(instance, Blueprint::new));

    public static final Codec<BlueprintRunSnapshot> RUN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BLUEPRINT_CODEC.fieldOf("blueprint").forGetter(BlueprintRunSnapshot::blueprint),
                    Codec.LONG.fieldOf("total_runs").forGetter(BlueprintRunSnapshot::totalRuns),
                    Codec.LONG.fieldOf("to_dispatch").forGetter(BlueprintRunSnapshot::toDispatch),
                    NexusResources.AMOUNT_CODEC.listOf().fieldOf("awaited").forGetter(BlueprintRunSnapshot::awaited))
            .apply(instance, BlueprintRunSnapshot::new));

    public static final Codec<CraftingTaskSnapshot> TASK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(CraftingTaskSnapshot::id),
                    NexusResources.AMOUNT_CODEC.fieldOf("target").forGetter(CraftingTaskSnapshot::target),
                    Codec.STRING.optionalFieldOf("requester", "").forGetter(CraftingTaskSnapshot::requester),
                    enumCodec(TaskState.values()).fieldOf("state").forGetter(CraftingTaskSnapshot::state),
                    NexusResources.AMOUNT_CODEC.listOf().fieldOf("held").forGetter(CraftingTaskSnapshot::held),
                    NexusResources.AMOUNT_CODEC.listOf().fieldOf("to_gather").forGetter(CraftingTaskSnapshot::toGather),
                    RUN_CODEC.listOf().fieldOf("runs").forGetter(CraftingTaskSnapshot::runs))
            .apply(instance, CraftingTaskSnapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TaskEntry> ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    NexusResources.STREAM_CODEC, entry -> NexusResources.of(entry.resource()),
                    ByteBufCodecs.VAR_LONG, TaskEntry::held,
                    ByteBufCodecs.VAR_LONG, TaskEntry::scheduled,
                    ByteBufCodecs.VAR_LONG, TaskEntry::processing,
                    NeoForgeStreamCodecs.enumCodec(DispatchResult.class), TaskEntry::problem,
                    TaskEntry::new);

    /** A task status as a crafting monitor receives it; its resources are resources of the game. */
    public static final StreamCodec<RegistryFriendlyByteBuf, TaskStatus> STATUS_STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, TaskStatus::id,
                    NexusResources.AMOUNT_STREAM_CODEC, TaskStatus::target,
                    ByteBufCodecs.STRING_UTF8, TaskStatus::requester,
                    NeoForgeStreamCodecs.enumCodec(TaskState.class), TaskStatus::state,
                    ByteBufCodecs.DOUBLE, TaskStatus::progress,
                    ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), TaskStatus::entries,
                    TaskStatus::new);

    private BlueprintCodecs() {
    }

    private static <E extends Enum<E>> Codec<E> enumCodec(final E[] values) {
        return Codec.STRING.comapFlatMap(name -> {
            for (E value : values) {
                if (nameOf(value).equals(name)) {
                    return DataResult.success(value);
                }
            }
            return DataResult.error(() -> "unknown value " + name);
        }, BlueprintCodecs::nameOf);
    }

    private static String nameOf(final Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
