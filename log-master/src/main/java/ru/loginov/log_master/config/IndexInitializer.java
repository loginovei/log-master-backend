package ru.loginov.log_master.config;

import jakarta.annotation.PostConstruct;
import jakarta.json.stream.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer {

    private static final String MAPPINGS_PATH_PREFIX = "/opensearch/mappings/";
    private static final String MAPPINGS_PATH_SUFFIX = ".json";
    private static final String ALL_INDICES_PATTERN = "*";

    private final OpenSearchClient client;
    private final IndexProperties properties;

    @PostConstruct
    public void initIndices() {
        Set<String> existing = fetchExistingIndices();
        log.debug("Existing OpenSearch indices: {}", existing);

        for (String index : properties.getIndices()) {
            if (existing.contains(index)) {
                log.debug("OpenSearch index already exists: {}", index);
                continue;
            }
            try {
                create(index);
                log.info("Created OpenSearch index: {}", index);
            } catch (Exception e) {
                log.error("Failed to create OpenSearch index: {}", index, e);
            }
        }
    }

    private Set<String> fetchExistingIndices() {
        try {
            return client.indices().get(g -> g.index(ALL_INDICES_PATTERN)).result().keySet();
        } catch (Exception e) {
            // OpenSearch returns 404 when no indices exist yet
            log.debug("No existing indices found ({})", e.getMessage());
            return Set.of();
        }
    }

    private void create(String name) throws IOException {
        TypeMapping mapping = loadMapping(name);
        if (mapping != null) {
            client.indices().create(c -> c.index(name).mappings(mapping));
        } else {
            client.indices().create(c -> c.index(name));
        }
    }

    private TypeMapping loadMapping(String name) {
        String path = MAPPINGS_PATH_PREFIX + name + MAPPINGS_PATH_SUFFIX;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                log.debug("No mapping file found for index: {}", name);
                return null;
            }
            JsonpMapper mapper = client._transport().jsonpMapper();
            JsonParser parser = mapper.jsonProvider().createParser(is);
            return TypeMapping._DESERIALIZER.deserialize(parser, mapper);
        } catch (Exception e) {
            log.warn("Could not load mapping for index '{}': {}", name, e.getMessage());
            return null;
        }
    }
}
