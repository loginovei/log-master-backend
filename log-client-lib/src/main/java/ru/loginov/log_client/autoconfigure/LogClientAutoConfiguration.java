package ru.loginov.log_client.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.loginov.log_client.HttpLogSink;
import ru.loginov.log_client.LogClient;
import ru.loginov.log_client.LogClientProperties;
import ru.loginov.log_client.LogSink;

@AutoConfiguration
@EnableConfigurationProperties(LogClientProperties.class)
@ConditionalOnProperty(prefix = "log-master.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogClientAutoConfiguration {

    /** Создаётся только если никакой другой LogSink не зарегистрирован в контексте. */
    @Bean
    @ConditionalOnMissingBean(LogSink.class)
    public HttpLogSink httpLogSink(LogClientProperties props) {
        return new HttpLogSink(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogClient logClient(LogClientProperties props, LogSink sink) {
        return new LogClient(props, sink);
    }
}
