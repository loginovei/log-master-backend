package ru.loginov.log_client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fluent builder for a single log entry. Obtained via {@link LogClient#log(LogLevel)}. */
public class LogMessageBuilder {

    private final LogClient client;
    private final LogLevel level;
    private String logCode;
    private final List<Object> args = new ArrayList<>();
    private String additional;
    private String exception;

    LogMessageBuilder(LogClient client, LogLevel level) {
        this.client = client;
        this.level = level;
    }

    public LogMessageBuilder code(String logCode) {
        this.logCode = logCode;
        return this;
    }

    public LogMessageBuilder arg(Object... values) {
        args.addAll(Arrays.asList(values));
        return this;
    }

    public LogMessageBuilder additional(String additional) {
        this.additional = additional;
        return this;
    }

    public LogMessageBuilder exception(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        this.exception = sw.toString();
        return this;
    }

    public void send() {
        if (logCode == null || logCode.isBlank()) {
            throw new IllegalStateException("logCode must be set before send()");
        }
        client.enqueue(level, logCode, args, exception, additional);
    }
}
