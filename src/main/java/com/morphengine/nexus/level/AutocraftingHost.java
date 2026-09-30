package com.morphengine.nexus.level;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.automation.CraftingTask;
import com.morphengine.nexus.automation.TaskList;
import com.morphengine.nexus.storage.InsertInterceptor;

import java.util.List;
import java.util.UUID;

/**
 * A network member that runs blueprints and keeps crafting tasks, such as an
 * Assembler. It offers its blueprints to the network's {@link
 * AutocraftingComponent} and tells it through {@link
 * AutocraftingComponent#refresh} when they or their priority change. As an
 * interceptor it lets its tasks claim the outputs they wait for.
 */
public interface AutocraftingHost extends NetworkMember, BlueprintExecutor, InsertInterceptor {

    /**
     * @return the blueprints the host runs now
     */
    List<Blueprint> blueprints();

    /**
     * @return priority of the host's blueprints; higher is preferred
     */
    int blueprintPriority();

    /**
     * @return the tasks the host keeps and moves on
     */
    TaskList craftingTasks();

    /**
     * Takes over a task the network started; the host keeps and moves it on from now on.
     */
    void adoptTask(CraftingTask task);

    default List<TaskStatus> taskStatuses() {
        return craftingTasks().statuses();
    }

    /**
     * @return whether the host kept a task of that id and cancelled it
     */
    default boolean cancelTask(final UUID id) {
        return craftingTasks().cancel(id);
    }

    /**
     * @return units of {@code resource} the runs of the host's tasks still owe them
     */
    default long awaited(final ResourceKey resource) {
        return craftingTasks().awaited(resource);
    }

    /**
     * @return whether one of the host's tasks is crafting {@code resource} now
     */
    default boolean isCrafting(final ResourceKey resource) {
        return craftingTasks().isCrafting(resource);
    }

    @Override
    default long intercept(final ResourceKey resource, final long amount, final Action action) {
        return craftingTasks().intercept(resource, amount, action);
    }

    @Override
    default long inserted(final ResourceKey resource, final long amount) {
        return craftingTasks().inserted(resource, amount);
    }
}
