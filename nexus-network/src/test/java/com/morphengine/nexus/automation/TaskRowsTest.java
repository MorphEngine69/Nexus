package com.morphengine.nexus.automation;

import com.morphengine.nexus.api.automation.TaskState;
import com.morphengine.nexus.api.automation.TaskStatus;
import com.morphengine.nexus.api.resource.ResourceAmount;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskRowsTest {

    private static TaskStatus task(final double progress) {
        return new TaskStatus(UUID.randomUUID(), new ResourceAmount(STONE, 1), "", TaskState.RUNNING, progress,
                List.of());
    }

    @Test
    void noTasksLeavesEveryRowEmpty() {
        final int rows = TaskRows.pack(List.of());

        assertThat(IntStream.range(0, TaskRows.ROWS)).noneMatch(row -> TaskRows.hasTask(rows, row));
    }

    @Test
    void taskThatHasNotStartedHasARowWithNoSegmentLit() {
        final int rows = TaskRows.pack(List.of(task(0)));

        assertThat(TaskRows.hasTask(rows, 0)).isTrue();
        assertThat(TaskRows.segmentsOf(rows, 0)).isZero();
    }

    @Test
    void anyProgressLightsAtLeastOneSegment() {
        assertThat(TaskRows.segmentsOf(TaskRows.pack(List.of(task(0.01))), 0)).isEqualTo(1);
    }

    @Test
    void halfwayLightsThreeOfFiveSegments() {
        assertThat(TaskRows.segmentsOf(TaskRows.pack(List.of(task(0.5))), 0)).isEqualTo(3);
    }

    @Test
    void finishedTaskLightsEverySegment() {
        assertThat(TaskRows.segmentsOf(TaskRows.pack(List.of(task(1))), 0)).isEqualTo(TaskRows.SEGMENTS);
    }

    @Test
    void rowsFollowTheOrderOfTheTasks() {
        final int rows = TaskRows.pack(List.of(task(1), task(0.2), task(0)));

        assertThat(TaskRows.segmentsOf(rows, 0)).isEqualTo(5);
        assertThat(TaskRows.segmentsOf(rows, 1)).isEqualTo(1);
        assertThat(TaskRows.segmentsOf(rows, 2)).isZero();
    }

    @Test
    void tasksBeyondTheLastRowAreLeftOut() {
        final int rows = TaskRows.pack(List.of(task(1), task(1), task(1), task(1)));

        assertThat(IntStream.range(0, TaskRows.ROWS)).allMatch(row -> TaskRows.hasTask(rows, row));
    }

    @Test
    void emptyRowAfterTheLastTaskHasNoTask() {
        final int rows = TaskRows.pack(List.of(task(0.4)));

        assertThat(TaskRows.hasTask(rows, 1)).isFalse();
        assertThat(TaskRows.segmentsOf(rows, 1)).isZero();
    }

    @Test
    void rowOutOfRangeIsRejected() {
        assertThatThrownBy(() -> TaskRows.hasTask(0, TaskRows.ROWS))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("row 3");
    }
}
