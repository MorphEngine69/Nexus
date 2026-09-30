package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order in which the inputs of a blueprint get their resources, when
 * planning and when handing out a run: those accepting fewer resources first,
 * so an input with substitutes does not take what only a narrower one accepts.
 */
final class InputOrder {

    private static final Comparator<BlueprintInput> NARROWEST_FIRST =
            Comparator.comparingInt(input -> input.options().size());

    private InputOrder() {
    }

    static List<BlueprintInput> narrowestFirst(final Blueprint blueprint) {
        final List<BlueprintInput> inputs = new ArrayList<>(blueprint.inputs());
        inputs.sort(NARROWEST_FIRST);
        return List.copyOf(inputs);
    }
}
