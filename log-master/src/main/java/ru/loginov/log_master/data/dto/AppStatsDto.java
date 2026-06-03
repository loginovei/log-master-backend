package ru.loginov.log_master.data.dto;

import java.util.List;
import java.util.Map;

/** Статистика по логам для дашборда. */
public record AppStatsDto(
        long totalTemplates,
        long totalEntries,
        Map<String, Long> entriesPerLevel,
        Map<String, Long> entriesPerService,
        List<DailyActivity> recentActivity
) {
    public record DailyActivity(String date, long count) {}
}
