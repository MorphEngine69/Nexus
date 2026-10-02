package com.morphengine.nexus.level;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintExecutor;
import com.morphengine.nexus.api.automation.BlueprintProvider;
import com.morphengine.nexus.api.automation.CraftingPlan;
import com.morphengine.nexus.api.automation.PlannedRuns;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.StorageView;
import com.morphengine.nexus.automation.BlueprintRegistry;
import com.morphengine.nexus.automation.CraftingPlanner;
import com.morphengine.nexus.automation.CraftingTask;
import com.morphengine.nexus.storage.InsertInterceptor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Autocrafting of one network: the blueprints its {@link AutocraftingHost}s
 * offer, planning, starting and cancelling tasks, and the outputs those tasks
 * wait for. It sees every insert into the network's storage and lets the
 * hosts' tasks claim what they wait for, the host that joined first first.
 */
public final class AutocraftingComponent implements NetworkComponent, InsertInterceptor {

    private final BlueprintRegistry registry = new BlueprintRegistry();
    private final List<AutocraftingHost> hosts = new ArrayList<>();

    @Override
    public void adopt(final List<NetworkMember> members) {
        final List<AutocraftingHost> present = new ArrayList<>();
        for (NetworkMember member : members) {
            if (member instanceof AutocraftingHost host) {
                present.add(host);
            }
        }
        for (AutocraftingHost host : hosts) {
            if (!present.contains(host)) {
                registry.withdraw(host);
            }
        }
        hosts.clear();
        hosts.addAll(present);
        for (AutocraftingHost host : present) {
            registry.offer(host, host.blueprints(), host.blueprintPriority());
        }
    }

    /**
     * Brings the network up to date with the blueprints and priority {@code host} offers now.
     */
    public void refresh(final AutocraftingHost host) {
        Objects.requireNonNull(host, "host must not be null");
        if (hosts.contains(host)) {
            registry.offer(host, host.blueprints(), host.blueprintPriority());
        }
    }

    /**
     * Takes {@code host} out at once, for a host that is being removed.
     */
    public void detach(final AutocraftingHost host) {
        if (hosts.remove(host)) {
            registry.withdraw(host);
        }
    }

    public BlueprintProvider blueprints() {
        return registry;
    }

    /**
     * @return a number that changes whenever the blueprints of the network change
     */
    public int revision() {
        return registry.revision();
    }

    /**
     * @param storage the network's storage, read as it stands
     */
    public CraftingPlan plan(final ResourceKey resource, final long amount, final StorageView storage) {
        return new CraftingPlanner(registry, storage).plan(resource, amount);
    }

    /**
     * @param storage the network's storage, read as it stands
     * @return the plan for the most of {@code resource}, up to {@code amount},
     *         that can start; see {@link CraftingPlanner#planLargest}
     */
    public CraftingPlan planLargest(final ResourceKey resource, final long amount, final StorageView storage) {
        return new CraftingPlanner(registry, storage).planLargest(resource, amount);
    }

    /**
     * Starts {@code plan} as a task kept by the host whose blueprint gives
     * the resource asked for.
     *
     * @param requester name of whoever asked for it; empty when nobody in particular did
     * @return whether the task started; not when the plan is incomplete or no host is left
     */
    public boolean start(final CraftingPlan plan, final String requester) {
        final AutocraftingHost host = hostFor(plan);
        if (!plan.isComplete() || host == null) {
            return false;
        }
        host.adoptTask(CraftingTask.start(plan, requester));
        return true;
    }

    public List<TaskStatus> statuses() {
        final List<TaskStatus> statuses = new ArrayList<>();
        for (AutocraftingHost host : hosts) {
            statuses.addAll(host.taskStatuses());
        }
        return statuses;
    }

    /**
     * @return whether a task of that id was found and cancelled
     */
    public boolean cancel(final UUID id) {
        for (AutocraftingHost host : hosts) {
            if (host.cancelTask(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return units of {@code resource} runs handed out by the network's tasks still owe them
     */
    public long awaited(final ResourceKey resource) {
        long owed = 0;
        for (int i = 0; i < hosts.size(); i++) {
            owed += hosts.get(i).awaited(resource);
        }
        return owed;
    }

    public boolean isCrafting(final ResourceKey resource) {
        for (AutocraftingHost host : hosts) {
            if (host.isCrafting(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long intercept(final ResourceKey resource, final long amount, final Action action) {
        long claimed = 0;
        for (int i = 0; i < hosts.size() && claimed < amount; i++) {
            claimed += hosts.get(i).intercept(resource, amount - claimed, action);
        }
        return claimed;
    }

    @Override
    public long inserted(final ResourceKey resource, final long amount) {
        long counted = 0;
        for (int i = 0; i < hosts.size() && counted < amount; i++) {
            counted += hosts.get(i).inserted(resource, amount - counted);
        }
        return counted;
    }

    private @Nullable AutocraftingHost hostFor(final CraftingPlan plan) {
        final ResourceKey target = plan.target().resource();
        for (PlannedRuns runs : plan.runs()) {
            final Blueprint blueprint = runs.blueprint();
            if (blueprint.outputOf(target) == 0) {
                continue;
            }
            for (BlueprintExecutor executor : registry.executorsFor(blueprint)) {
                if (executor instanceof AutocraftingHost host && hosts.contains(host)) {
                    return host;
                }
            }
        }
        return hosts.isEmpty() ? null : hosts.getFirst();
    }
}
