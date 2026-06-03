package ru.loginov.log_client;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Асинхронный клиент для отправки лог-записей через {@link LogSink}.
 *
 * <p>Записи буферизуются во внутренней очереди и отправляются батчами
 * в отдельном потоке. Не блокирует вызывающий поток.
 *
 * <p>Пример использования:
 * <pre>{@code
 * // Краткий вариант
 * logClient.info("USER_LOGIN", userId);
 * logClient.error("AUTH_FAIL", ex, userId, ip);
 *
 * logClient.log(LogLevel.WARN)
 *          .code("RATE_LIMIT")
 *          .arg(clientId, limit)
 *          .additional("endpoint=/api/data")
 *          .send();
 * }</pre>
 */
public class LogClient implements Closeable {

    private final LogClientProperties props;
    private final LogSink sink;
    private final LinkedBlockingQueue<LogEntryRequest> queue;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong droppedCount = new AtomicLong(0);

    public LogClient(LogClientProperties props, LogSink sink) {
        this.props = props;
        this.sink = sink;
        this.queue = new LinkedBlockingQueue<>(props.getQueueCapacity());

        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.setThreadFactory(r -> {
            Thread t = new Thread(r, "log-client-worker");
            t.setDaemon(true);
            return t;
        });
        exec.setRemoveOnCancelPolicy(true);
        this.scheduler = exec;
        this.scheduler.scheduleWithFixedDelay(
                this::flush,
                props.getFlushIntervalMs(),
                props.getFlushIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }

    // ── Builder API ───────────────────────────────────────────────────────────

    public LogMessageBuilder log(LogLevel level) {
        return new LogMessageBuilder(this, level);
    }

    // ── Convenience level methods ─────────────────────────────────────────────

    public void trace(String logCode, Object... args) {
        enqueue(LogLevel.TRACE, logCode, Arrays.asList(args), null, null);
    }

    public void debug(String logCode, Object... args) {
        enqueue(LogLevel.DEBUG, logCode, Arrays.asList(args), null, null);
    }

    public void info(String logCode, Object... args) {
        enqueue(LogLevel.INFO, logCode, Arrays.asList(args), null, null);
    }

    public void warn(String logCode, Object... args) {
        enqueue(LogLevel.WARN, logCode, Arrays.asList(args), null, null);
    }

    public void error(String logCode, Object... args) {
        enqueue(LogLevel.ERROR, logCode, Arrays.asList(args), null, null);
    }

    public void error(String logCode, Throwable t, Object... args) {
        enqueue(LogLevel.ERROR, logCode, Arrays.asList(args), stackTraceOf(t), null);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    void enqueue(LogLevel level, String logCode, List<Object> args, String exception, String additional) {
        LogEntryRequest entry = new LogEntryRequest(
                logCode,
                props.getAppCode(),
                Instant.now(),
                exception,
                additional,
                args,
                level.name()
        );
        consoleLog(level, logCode, args, exception);
        if (!queue.offer(entry)) {
            long dropped = droppedCount.incrementAndGet();
            if (dropped == 1 || dropped % 1000 == 0) {
                System.out.printf("[log-client] WARN  %s  QUEUE_FULL  dropped=%d%n",
                        props.getAppCode(), dropped);
            }
        }
    }

    private void consoleLog(LogLevel level, String logCode, List<Object> args, String exception) {
        String argsStr = args != null && !args.isEmpty() ? " " + args : "";
        System.out.printf("[log-client] %-5s %s  %s%s%n", level.name(), props.getAppCode(), logCode, argsStr);
        if (exception != null && !exception.isBlank()) {
            System.out.println(exception);
        }
    }

    private void flush() {
        List<LogEntryRequest> batch = new ArrayList<>(props.getBatchSize());
        queue.drainTo(batch, props.getBatchSize());
        if (batch.isEmpty()) return;
        sink.send(batch);
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        flush();
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
