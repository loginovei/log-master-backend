package ru.loginov.log_master.repository;

import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.json.JsonData;
import org.springframework.stereotype.Repository;
import ru.loginov.log_master.data.dto.AppStatsDto;
import ru.loginov.log_master.data.dto.PageDto;
import ru.loginov.log_master.data.model.LogEntry;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LogEntryRepository {

    static final String INDEX = "log-entries";

    private static final String FIELD_APP_CODE = "applicationCode";
    private static final String FIELD_CODE     = "code";
    private static final String FIELD_LEVEL    = "level";
    private static final String FIELD_CREATED  = "created";

    private static final String AGG_BY_LEVEL   = "by_level";
    private static final String AGG_BY_SERVICE = "by_service";
    private static final String AGG_BY_DATE    = "by_date";

    private static final String DATE_FORMAT          = "yyyy-MM-dd";
    private static final int    LEVEL_AGG_SIZE        = 10;
    private static final int    SERVICE_AGG_SIZE      = 100;
    private static final int    RECENT_ACTIVITY_DAYS  = 5;

    private static final String ERR_SEARCH     = "Failed to search log entries";
    private static final String ERR_SAVE       = "Failed to save log entry";
    private static final String ERR_SAVE_BATCH = "Failed to bulk save log entries";
    private static final String ERR_COUNT      = "Failed to count log entries";
    private static final String ERR_AGG        = "Failed to aggregate stats";

    private final OpenSearchClient client;

    /** Постраничный поиск записей логов с фильтрами. */
    public PageDto<LogEntry> search(String appCode, List<String> logCodes, String level,
                                    String from, String to, String argsQuery, int page, int size) {
        try {
            SearchResponse<LogEntry> resp = client.search(s -> {
                s.index(INDEX)
                        .from(page * size)
                        .size(size)
                        .trackTotalHits(t -> t.enabled(true))
                        .sort(so -> so.field(f -> f.field(FIELD_CREATED).order(SortOrder.Desc)));
                s.query(q -> q.bool(b -> {
                    if (appCode != null && !appCode.isBlank()) {
                        b.filter(f -> f.term(t -> t.field(FIELD_APP_CODE).value(FieldValue.of(appCode))));
                    }
                    if (logCodes != null && !logCodes.isEmpty()) {
                        List<FieldValue> values = logCodes.stream().map(FieldValue::of).toList();
                        b.filter(f -> f.terms(t -> t.field(FIELD_CODE).terms(tv -> tv.value(values))));
                    }
                    if (level != null && !level.isBlank()) {
                        b.filter(f -> f.term(t -> t.field(FIELD_LEVEL).value(FieldValue.of(level))));
                    }
                    if (from != null && !from.isBlank()) {
                        b.filter(f -> f.range(r -> r.field(FIELD_CREATED).gte(JsonData.of(from))));
                    }
                    if (to != null && !to.isBlank()) {
                        b.filter(f -> f.range(r -> r.field(FIELD_CREATED).lte(JsonData.of(to))));
                    }
                    if (argsQuery != null && !argsQuery.isBlank()) {
                        b.must(m -> m.multiMatch(mm -> mm.query(argsQuery).fields("args.*")));
                    }
                    return b;
                }));
                return s;
            }, LogEntry.class);

            List<LogEntry> content = resp.hits().hits().stream()
                    .map(hit -> {
                        LogEntry e = hit.source();
                        if (e != null) e.setId(hit.id());
                        return e;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            long total = resp.hits().total() != null ? resp.hits().total().value() : 0;
            return new PageDto<>(content, total, (int) Math.ceil((double) total / size), page, size);
        } catch (Exception e) {
            throw new RuntimeException(ERR_SEARCH, e);
        }
    }

    /** Сохраняет запись лога (ID генерирует OpenSearch). */
    public LogEntry save(LogEntry entry) {
        try {
            var resp = client.index(i -> i.index(INDEX).document(entry));
            entry.setId(resp.id());
            return entry;
        } catch (IOException e) {
            throw new RuntimeException(ERR_SAVE, e);
        }
    }

    /** Сохраняет список записей одним bulk-запросом. */
    public List<LogEntry> saveBatch(List<LogEntry> entries) {
        if (entries.isEmpty()) return List.of();
        try {
            List<BulkOperation> ops = entries.stream()
                    .map(e -> BulkOperation.of(op -> op.index(idx -> idx.index(INDEX).document(e))))
                    .toList();
            BulkResponse resp = client.bulk(b -> b.operations(ops));
            if (resp.errors()) {
                String errors = resp.items().stream()
                        .filter(i -> i.error() != null)
                        .map(i -> i.error().reason())
                        .collect(Collectors.joining("; "));
                throw new RuntimeException(ERR_SAVE_BATCH + ": " + errors);
            }
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setId(resp.items().get(i).id());
            }
            return entries;
        } catch (IOException e) {
            throw new RuntimeException(ERR_SAVE_BATCH, e);
        }
    }

    /** Считает количество записей с опциональным фильтром по приложению. */
    public long count(String appCode) {
        try {
            CountResponse resp = client.count(c -> {
                c.index(INDEX);
                if (appCode != null && !appCode.isBlank()) {
                    c.query(q -> q.term(t -> t.field(FIELD_APP_CODE).value(FieldValue.of(appCode))));
                }
                return c;
            });
            return resp.count();
        } catch (IOException e) {
            throw new RuntimeException(ERR_COUNT, e);
        }
    }

    /**
     * Возвращает агрегированную статистику: записей по уровням, по сервисам
     * и активность за последние 30 дней (5 последних дней с данными).
     */
    public StatsAggResult aggregateStats(String appCode) {
        try {
            SearchResponse<Void> resp = client.search(s -> {
                s.index(INDEX).size(0);
                if (appCode != null && !appCode.isBlank()) {
                    s.query(q -> q.term(t -> t.field(FIELD_APP_CODE).value(FieldValue.of(appCode))));
                } else {
                    s.query(q -> q.matchAll(m -> m));
                }
                s.aggregations(AGG_BY_LEVEL,   a -> a.terms(t -> t.field(FIELD_LEVEL).size(LEVEL_AGG_SIZE)));
                s.aggregations(AGG_BY_SERVICE, a -> a.terms(t -> t.field(FIELD_APP_CODE).size(SERVICE_AGG_SIZE)));
                s.aggregations(AGG_BY_DATE,    a -> a.dateHistogram(d -> d
                        .field(FIELD_CREATED)
                        .calendarInterval(CalendarInterval.Day)
                        .format(DATE_FORMAT)
                        .minDocCount(1)));
                return s;
            }, Void.class);

            Map<String, Long> byLevel = resp.aggregations().get(AGG_BY_LEVEL)
                    .sterms().buckets().array().stream()
                    .collect(Collectors.toMap(StringTermsBucket::key, StringTermsBucket::docCount));

            Map<String, Long> byService = resp.aggregations().get(AGG_BY_SERVICE)
                    .sterms().buckets().array().stream()
                    .collect(Collectors.toMap(StringTermsBucket::key, StringTermsBucket::docCount));

            List<AppStatsDto.DailyActivity> activity = resp.aggregations().get(AGG_BY_DATE)
                    .dateHistogram().buckets().array().stream()
                    .sorted((a, b) -> b.keyAsString().compareTo(a.keyAsString()))
                    .limit(RECENT_ACTIVITY_DAYS)
                    .sorted((a, b) -> a.keyAsString().compareTo(b.keyAsString()))
                    .map(b -> new AppStatsDto.DailyActivity(b.keyAsString(), b.docCount()))
                    .toList();

            return new StatsAggResult(byLevel, byService, activity);
        } catch (IOException e) {
            throw new RuntimeException(ERR_AGG, e);
        }
    }

    public record StatsAggResult(
            Map<String, Long> byLevel,
            Map<String, Long> byService,
            List<AppStatsDto.DailyActivity> recentActivity
    ) {}
}
