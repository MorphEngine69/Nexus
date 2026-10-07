package com.morphengine.nexus.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanScheduleTest {

    private final ScanSchedule schedule = new ScanSchedule();

    @Test
    void startsAtTheBaseInterval() {
        assertThat(schedule.intervalTicks(0)).isEqualTo(ScanSchedule.BASE_INTERVAL_TICKS);
    }

    @Test
    void largerBlockWaitsLonger() {
        schedule.scanned(new ExternalStorage.Scan(64, 0));

        assertThat(schedule.intervalTicks(0)).isEqualTo(ScanSchedule.BASE_INTERVAL_TICKS + 64 / 8);
    }

    @Test
    void unchangedBlockIsReadJustAsOften() {
        schedule.scanned(new ExternalStorage.Scan(10, 0));
        schedule.scanned(new ExternalStorage.Scan(10, 0));

        assertThat(schedule.intervalTicks(0)).isEqualTo(ScanSchedule.BASE_INTERVAL_TICKS + 10 / 8);
    }

    @Test
    void waitNeverPassesTheMaximum() {
        schedule.scanned(new ExternalStorage.Scan(1_000_000, 0));

        assertThat(schedule.intervalTicks(0)).isEqualTo(ScanSchedule.MAX_INTERVAL_TICKS);
    }

    @Test
    void watchedBlockIsReadOften() {
        schedule.scanned(new ExternalStorage.Scan(1_000, 0));
        schedule.watch(100);

        assertThat(schedule.intervalTicks(101)).isEqualTo(ScanSchedule.WATCH_INTERVAL_TICKS);
    }

    @Test
    void watchEndsAfterItsDuration() {
        schedule.watch(100);

        assertThat(schedule.intervalTicks(100 + ScanSchedule.WATCH_DURATION_TICKS))
                .isEqualTo(ScanSchedule.BASE_INTERVAL_TICKS);
    }
}
