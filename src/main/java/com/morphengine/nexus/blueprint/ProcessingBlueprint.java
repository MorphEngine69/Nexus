package com.morphengine.nexus.blueprint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Processing in a machine: the inputs an Assembler puts into it and the
 * outputs it waits for, items and fluids alike.
 *
 * @param inputs       what one run puts into the machine, in the order listed
 * @param outputs      what one run gives; the first is the main product
 * @param substitution whether an input also takes the other members of its tag
 */
public record ProcessingBlueprint(List<ProcessingInput> inputs, List<ResourceAmount> outputs,
                                  Substitution substitution) implements EncodedBlueprint {

    /** Slots of the encoder: a 3 by 3 grid of inputs and a column of outputs. */
    public static final int MAX_INPUTS = 9;
    public static final int MAX_OUTPUTS = 3;

    public static final MapCodec<ProcessingBlueprint> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            ProcessingInput.CODEC.listOf().fieldOf("inputs").forGetter(ProcessingBlueprint::inputs),
                            NexusResources.AMOUNT_CODEC.listOf().fieldOf("outputs")
                                    .forGetter(ProcessingBlueprint::outputs),
                            Substitution.CODEC.optionalFieldOf("substitution", Substitution.EXACT)
                                    .forGetter(ProcessingBlueprint::substitution))
                    .apply(instance, ProcessingBlueprint::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingBlueprint> STREAM_CODEC =
            StreamCodec.composite(
                    ProcessingInput.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INPUTS)),
                    ProcessingBlueprint::inputs,
                    NexusResources.AMOUNT_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_OUTPUTS)),
                    ProcessingBlueprint::outputs,
                    Substitution.STREAM_CODEC, ProcessingBlueprint::substitution,
                    ProcessingBlueprint::new);

    public ProcessingBlueprint {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        Objects.requireNonNull(substitution, "substitution must not be null");
        if (inputs.isEmpty() || outputs.isEmpty() || inputs.size() > MAX_INPUTS || outputs.size() > MAX_OUTPUTS) {
            throw new IllegalArgumentException("processing blueprint needs 1 to " + MAX_INPUTS + " inputs and 1 to "
                    + MAX_OUTPUTS + " outputs: inputs=" + inputs + ", outputs=" + outputs);
        }
    }

    /**
     * @return processing that takes nothing but the resources listed
     */
    public static ProcessingBlueprint exact(final List<ResourceAmount> inputs, final List<ResourceAmount> outputs) {
        final List<ProcessingInput> exactInputs = new ArrayList<>(inputs.size());
        for (ResourceAmount input : inputs) {
            exactInputs.add(ProcessingInput.of(input));
        }
        return new ProcessingBlueprint(exactInputs, outputs, Substitution.EXACT);
    }

    @Override
    public BlueprintKind kind() {
        return BlueprintKind.PROCESSING;
    }

    @Override
    public Blueprint blueprint() {
        final List<BlueprintInput> blueprintInputs = new ArrayList<>(inputs.size());
        for (ProcessingInput input : inputs) {
            blueprintInputs.add(new BlueprintInput(input.options(substitution), input.amount().amount()));
        }
        return new Blueprint(BlueprintKind.PROCESSING, blueprintInputs, outputs);
    }
}
