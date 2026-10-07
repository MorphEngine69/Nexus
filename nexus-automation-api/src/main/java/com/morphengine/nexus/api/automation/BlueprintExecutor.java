package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;

/**
 * Something that runs blueprints, such as an Assembler with the machine it
 * faces. The outputs of a run do not come back through the executor: they
 * reach the network like any other resource, and the crafting task waiting
 * for them claims them there. Server thread only.
 */
@FunctionalInterface
public interface BlueprintExecutor {

    /**
     * Hands the inputs of one run of {@code blueprint} to the executor, all or
     * nothing.
     *
     * @param inputs the resources picked for one run: for every
     *               {@linkplain Blueprint#inputs() input}, its amount made up of
     *               resources it accepts
     * @return {@link DispatchResult#ACCEPTED} when every input was taken, under
     *         {@link Action#SIMULATE} when every input would be; anything else
     *         when none was taken
     */
    DispatchResult dispatch(Blueprint blueprint, List<ResourceAmount> inputs, Action action);
}
