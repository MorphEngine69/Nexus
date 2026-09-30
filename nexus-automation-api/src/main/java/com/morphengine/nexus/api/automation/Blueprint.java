package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A recipe the network can run: what one run takes and what it gives. Two
 * blueprints with the same kind, inputs and outputs are the same blueprint,
 * wherever they are kept.
 *
 * @param inputs  what one run takes, inputs accepting the same resources once,
 *                in the order first listed
 * @param outputs what one run gives, each resource once; the first is the main product
 */
public record Blueprint(BlueprintKind kind, List<BlueprintInput> inputs, List<ResourceAmount> outputs) {

    /**
     * Lists inputs that accept the same resources, such as the planks of
     * several crafting slots, once with the amounts summed; the same for outputs.
     *
     * @throws IllegalArgumentException if there are no inputs or no outputs
     * @throws ArithmeticException      if a summed amount overflows a {@code long}
     */
    public Blueprint {
        Objects.requireNonNull(kind, "kind must not be null");
        inputs = mergedInputs(Objects.requireNonNull(inputs, "inputs must not be null"));
        outputs = merged(Objects.requireNonNull(outputs, "outputs must not be null"));
        if (inputs.isEmpty() || outputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "blueprint needs inputs and outputs: inputs=" + inputs + ", outputs=" + outputs);
        }
    }

    /**
     * @return a blueprint whose every input accepts nothing but the resource listed
     */
    public static Blueprint exact(final BlueprintKind kind, final List<ResourceAmount> inputs,
                                  final List<ResourceAmount> outputs) {
        final List<BlueprintInput> exactInputs = new ArrayList<>(inputs.size());
        for (ResourceAmount input : inputs) {
            exactInputs.add(BlueprintInput.of(input));
        }
        return new Blueprint(kind, exactInputs, outputs);
    }

    /**
     * @return the main product and how much of it one run gives
     */
    public ResourceAmount primaryOutput() {
        return outputs.getFirst();
    }

    /**
     * @return units of {@code resource} one run gives; zero when it gives none
     */
    public long outputOf(final ResourceKey resource) {
        for (ResourceAmount output : outputs) {
            if (output.resource().equals(resource)) {
                return output.amount();
            }
        }
        return 0;
    }

    private static List<BlueprintInput> mergedInputs(final List<BlueprintInput> inputs) {
        final Map<List<ResourceKey>, Long> totals = new LinkedHashMap<>();
        for (BlueprintInput input : inputs) {
            totals.merge(input.options(), input.amount(), Math::addExact);
        }
        final List<BlueprintInput> merged = new ArrayList<>(totals.size());
        for (Map.Entry<List<ResourceKey>, Long> total : totals.entrySet()) {
            merged.add(new BlueprintInput(total.getKey(), total.getValue()));
        }
        return List.copyOf(merged);
    }

    private static List<ResourceAmount> merged(final List<ResourceAmount> amounts) {
        final Map<ResourceKey, Long> totals = new LinkedHashMap<>();
        for (ResourceAmount amount : amounts) {
            totals.merge(amount.resource(), amount.amount(), Math::addExact);
        }
        final List<ResourceAmount> merged = new ArrayList<>(totals.size());
        for (Map.Entry<ResourceKey, Long> total : totals.entrySet()) {
            merged.add(new ResourceAmount(total.getKey(), total.getValue()));
        }
        return List.copyOf(merged);
    }
}
