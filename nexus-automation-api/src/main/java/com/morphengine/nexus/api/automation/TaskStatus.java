package com.morphengine.nexus.api.automation;

import com.morphengine.nexus.api.resource.ResourceAmount;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A crafting task as a crafting monitor shows it.
 *
 * @param id        stays the same for the whole life of the task, across saves
 * @param target    the resource asked for and how much of it
 * @param requester name of whoever asked for it; empty when nobody in particular did
 * @param progress  share of the outputs already received, from 0 to 1
 * @param entries   every resource the task holds, waits for or is held up on
 */
public record TaskStatus(
        UUID id, ResourceAmount target, String requester, TaskState state, double progress,
        List<TaskEntry> entries) {

    public TaskStatus {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(requester, "requester must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (progress < 0 || progress > 1) {
            throw new IllegalArgumentException("progress of task " + id + " must be within 0 and 1: " + progress);
        }
        entries = List.copyOf(entries);
    }

    /**
     * @return the same status in {@code changed}
     */
    public TaskStatus withState(final TaskState changed) {
        return new TaskStatus(id, target, requester, changed, progress, entries);
    }
}
