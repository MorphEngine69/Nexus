package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.DispatchResult;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.ArrayList;
import java.util.List;

/**
 * An executor that answers as told and remembers the runs it took.
 */
final class FakeExecutor implements BlueprintExecutor {

    private final List<Blueprint> taken = new ArrayList<>();
    private final List<List<ResourceAmount>> takenInputs = new ArrayList<>();
    private DispatchResult answer = DispatchResult.ACCEPTED;

    void answer(final DispatchResult result) {
        answer = result;
    }

    List<Blueprint> taken() {
        return taken;
    }

    /**
     * @return the resources handed over with each run taken, in order
     */
    List<List<ResourceAmount>> takenInputs() {
        return takenInputs;
    }

    @Override
    public DispatchResult dispatch(final Blueprint blueprint, final List<ResourceAmount> inputs, final Action action) {
        if (answer.isAccepted() && action.isExecute()) {
            taken.add(blueprint);
            takenInputs.add(List.copyOf(inputs));
        }
        return answer;
    }
}
