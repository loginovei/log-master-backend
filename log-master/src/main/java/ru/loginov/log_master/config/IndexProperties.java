package ru.loginov.log_master.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "opensearch")
@Data
public class IndexProperties {
    private List<String> indices = new ArrayList<>();
}
