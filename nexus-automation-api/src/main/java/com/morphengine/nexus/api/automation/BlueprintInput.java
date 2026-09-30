package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceKey;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One input of a blueprint: how many units one run takes, made up of any of
 * the resources it accepts, such as any planks where a recipe takes planks of
 * any wood. The units may mix several of them in one run.
 *
 * @param options the resources accepted, each once; the first is the one the
 *                blueprint was encoded with, preferred when crafting one
 * @param amount  units one run takes, always positive
 */
public record BlueprintInput(List<ResourceKey> options, long amount) {

    /**
     * @throws IllegalArgumentException if there are no options, one is given
     *                                  twice, or the amount is not positive
     */
    public BlueprintInput {
        options = List.copyOf(Objects.requireNonNull(options, "options must not be null"));
        if (options.isEmpty() || amount <= 0) {
            throw new IllegalArgumentException("input needs options and a positive amount: options=" + options
                    + ", amount=" + amount);
        }
        final Set<ResourceKey> seen = new HashSet<>();
        for (ResourceKey option : options) {
            if (!seen.add(option)) {
                throw new IllegalArgumentException("option " + option + " given twice: " + options);
            }
        }
    }

    /**
     * @return an input that accepts nothing but {@code exact}
     */
    public static BlueprintInput of(final ResourceAmount exact) {
        return new BlueprintInput(List.of(exact.resource()), exact.amount());
    }

    /**
     * @return the resource the blueprint was encoded with
     */
    public ResourceKey preferred() {
        return options.getFirst();
    }

    public boolean accepts(final ResourceKey resource) {
        return options.contains(resource);
    }

    /**
     * @return whether the input accepts more than one resource
     */
    public boolean hasSubstitutes() {
        return options.size() > 1;
    }
}
