package ru.loginov.log_master.repository;

import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Repository;
import ru.loginov.log_master.data.model.Application;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ApplicationRepository {

    static final String INDEX = "applications";

    private static final int FIND_ALL_SIZE = 100;

    private static final String ERR_FETCH_ALL = "Failed to fetch applications";
    private static final String ERR_FETCH     = "Failed to fetch application: ";
    private static final String ERR_SAVE      = "Failed to save application";
    private static final String ERR_DELETE    = "Failed to delete application: ";

    private final OpenSearchClient client;

    /** Возвращает все зарегистрированные приложения. */
    public List<Application> findAll() {
        try {
            SearchResponse<Application> resp = client.search(s -> s
                    .index(INDEX)
                    .size(FIND_ALL_SIZE)
                    .query(q -> q.matchAll(m -> m)),
                    Application.class);
            return resp.hits().hits().stream()
                    .map(hit -> {
                        Application app = hit.source();
                        if (app != null) app.setId(hit.id());
                        return app;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(ERR_FETCH_ALL, e);
        }
    }

    /** Находит приложение по коду. */
    public Optional<Application> findByCode(String code) {
        try {
            GetResponse<Application> resp = client.get(g -> g.index(INDEX).id(code), Application.class);
            if (!resp.found()) return Optional.empty();
            Application app = resp.source();
            if (app != null) app.setId(resp.id());
            return Optional.ofNullable(app);
        } catch (IOException e) {
            throw new RuntimeException(ERR_FETCH + code, e);
        }
    }

    /** Сохраняет (создаёт или обновляет) приложение. */
    public Application save(Application application) {
        try {
            client.index(i -> i.index(INDEX).id(application.getCode()).document(application));
            application.setId(application.getCode());
            return application;
        } catch (IOException e) {
            throw new RuntimeException(ERR_SAVE, e);
        }
    }

    /** Удаляет приложение по коду. */
    public void deleteByCode(String code) {
        try {
            client.delete(d -> d.index(INDEX).id(code));
        } catch (IOException e) {
            throw new RuntimeException(ERR_DELETE + code, e);
        }
    }
}
