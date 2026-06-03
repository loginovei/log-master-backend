package ru.loginov.log_master.repository;

import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Repository;
import ru.loginov.log_master.data.dto.PageDto;
import ru.loginov.log_master.data.model.LogTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LogTemplateRepository {

    static final String INDEX = "log-templates";

    private static final String FIELD_APP_CODE      = "applicationCode";
    private static final String FIELD_CODE          = "code";
    private static final String FIELD_MESSAGES_ALL  = "messages.*";
    private static final String FIELD_MESSAGES_PREFIX = "messages.";

    private static final int CACHE_LOAD_SIZE  = 10_000;
    private static final int DEFAULT_SEARCH_SIZE = 20;

    private static final String ERR_FETCH_ALL = "Failed to fetch templates";
    private static final String ERR_SEARCH    = "Failed to search templates";
    private static final String ERR_FETCH     = "Failed to fetch template: ";
    private static final String ERR_SAVE      = "Failed to save template";
    private static final String ERR_DELETE    = "Failed to delete template: ";
    private static final String ERR_COUNT     = "Failed to count templates";

    private final OpenSearchClient client;

    /** Возвращает постраничный список шаблонов с опциональным фильтром по приложению. */
    public PageDto<LogTemplate> findAll(String appCode, int page, int size) {
        try {
            SearchResponse<LogTemplate> resp = client.search(s -> {
                s.index(INDEX).from(page * size).size(size).trackTotalHits(t -> t.enabled(true));
                if (appCode != null && !appCode.isBlank()) {
                    s.query(q -> q.term(t -> t.field(FIELD_APP_CODE).value(FieldValue.of(appCode))));
                } else {
                    s.query(q -> q.matchAll(m -> m));
                }
                return s;
            }, LogTemplate.class);

            List<LogTemplate> content = resp.hits().hits().stream()
                    .map(hit -> {
                        LogTemplate t = hit.source();
                        if (t != null) t.setId(hit.id());
                        return t;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            long total = resp.hits().total() != null ? resp.hits().total().value() : 0;
            return new PageDto<>(content, total, (int) Math.ceil((double) total / size), page, size);
        } catch (IOException e) {
            throw new RuntimeException(ERR_FETCH_ALL, e);
        }
    }

    /** Возвращает все шаблоны без пагинации (используется для кеша). */
    public List<LogTemplate> findAll() {
        return findAll(null, 0, CACHE_LOAD_SIZE).content();
    }

    /** Полнотекстовый поиск по коду и текстам шаблонов на указанном языке. */
    public List<LogTemplate> search(String appCode, String q, String lang) {
        String messagesField = (lang != null && !lang.isBlank())
                ? FIELD_MESSAGES_PREFIX + lang
                : FIELD_MESSAGES_ALL;
        try {
            Query query = Query.of(sq -> sq.bool(b -> {
                if (appCode != null && !appCode.isBlank()) {
                    b.filter(f -> f.term(t -> t.field(FIELD_APP_CODE).value(FieldValue.of(appCode))));
                }
                if (q != null && !q.isBlank()) {
                    b.must(m -> m.multiMatch(mm -> mm
                            .fields(List.of(FIELD_CODE, messagesField))
                            .query(q)
                            .type(TextQueryType.PhrasePrefix)));
                } else {
                    b.must(m -> m.matchAll(ma -> ma));
                }
                return b;
            }));
            SearchResponse<LogTemplate> resp = client.search(s -> {
                s.index(INDEX).size(DEFAULT_SEARCH_SIZE).query(query);
                return s;
            }, LogTemplate.class);

            return resp.hits().hits().stream()
                    .map(hit -> {
                        LogTemplate t = hit.source();
                        if (t != null) t.setId(hit.id());
                        return t;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(ERR_SEARCH, e);
        }
    }

    /** Находит шаблон по логКоду. */
    public Optional<LogTemplate> findByCode(String code) {
        try {
            GetResponse<LogTemplate> resp = client.get(g -> g.index(INDEX).id(code), LogTemplate.class);
            if (!resp.found()) return Optional.empty();
            LogTemplate t = resp.source();
            if (t != null) t.setId(resp.id());
            return Optional.ofNullable(t);
        } catch (IOException e) {
            throw new RuntimeException(ERR_FETCH + code, e);
        }
    }

    /** Сохраняет (создаёт или обновляет) шаблон. */
    public LogTemplate save(LogTemplate template) {
        try {
            client.index(i -> i.index(INDEX).id(template.getCode()).document(template));
            template.setId(template.getCode());
            return template;
        } catch (IOException e) {
            throw new RuntimeException(ERR_SAVE, e);
        }
    }

    /** Удаляет шаблон по логКоду. */
    public void deleteByCode(String code) {
        try {
            client.delete(d -> d.index(INDEX).id(code));
        } catch (IOException e) {
            throw new RuntimeException(ERR_DELETE + code, e);
        }
    }

    /** Считает количество шаблонов с опциональным фильтром по приложению. */
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
}
