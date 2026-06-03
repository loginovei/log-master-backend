# log-client-lib

Spring Boot Starter для отправки структурированных лог-записей на сервер log-master.

## Требования

- Java 21+
- Spring Boot 4.x

## Подключение

Установите библиотеку в локальный Maven-репозиторий:

```bash
./mvnw install -DskipTests
```

Добавьте зависимость в `pom.xml` вашего приложения:

```xml
<dependency>
    <groupId>ru.loginov</groupId>
    <artifactId>log-client-lib</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Архитектура

```
LogClient  ──►  LogSink (interface)
                  │
                  ├── HttpLogSink        (дефолт: POST /api/logs/batch)
                  │
                  └── DirectLogSink      (в log-master: вызов LogService напрямую)
```

`LogClient` управляет только очередью и фоновым сбросом. Куда именно отправляется батч — определяет реализация `LogSink`.

**`HttpLogSink`** регистрируется авто-конфигурацией, если в Spring-контексте нет другого бина типа `LogSink`.

**`DirectLogSink`** живёт в самом `log-master`: принимает `List<LogEntryRequest>`, маппит в `List<LogEntryDto>` и вызывает `LogService.saveBatch()` без HTTP-прыжка. Как только этот бин появляется в контексте, авто-конфигурация пропускает создание `HttpLogSink`.

### Добавление собственной реализации

```java
@Component
public class KafkaLogSink implements LogSink {
    @Override
    public void send(List<LogEntryRequest> batch) {
        // отправить в Kafka
    }
}
```

Достаточно объявить бин — `HttpLogSink` создан не будет.

## Конфигурация

```yaml
log-master:
  client:
    enabled: true              # false — отключить бин (по умолчанию true)
    server-url: http://localhost:8080  # URL сервера (для HttpLogSink)
    app-code: my-service       # идентификатор сервиса
    queue-capacity: 10000      # размер буферной очереди
    batch-size: 50             # записей в одном вызове send()
    flush-interval-ms: 500     # интервал сброса очереди (мс)
```

| Свойство | По умолчанию | Описание |
|---|---|---|
| `server-url` | `http://localhost:8080` | URL сервера log-master (только для `HttpLogSink`) |
| `app-code` | `unknown` | Код сервиса, проставляется в каждую запись |
| `queue-capacity` | `10000` | При переполнении новые записи отбрасываются |
| `batch-size` | `50` | Максимум записей в одном вызове `LogSink.send()` |
| `flush-interval-ms` | `500` | Как часто фоновый поток сбрасывает очередь |

## Использование

`LogClient` регистрируется как Spring-бин и доступен через внедрение зависимостей:

```java
@Service
public class AuthService {

    private final LogClient logClient;

    public AuthService(LogClient logClient) {
        this.logClient = logClient;
    }

    public void login(String userId, String ip) {
        logClient.info("USER_LOGIN", userId);
    }

    public void failedLogin(String userId, String ip, Exception ex) {
        logClient.error("AUTH_FAIL", ex, userId, ip);
    }
}
```

### Краткие методы

```java
logClient.trace("CODE", arg1, arg2);
logClient.debug("CODE", arg1);
logClient.info("CODE", arg1, arg2);
logClient.warn("CODE", arg1);
logClient.error("CODE", arg1);                // без исключения
logClient.error("CODE", exception, arg1);     // со stack trace
```

### Fluent builder

Используйте `logClient.log(level)` для расширенных сценариев:

```java
logClient.log(LogLevel.WARN)
         .code("RATE_LIMIT")
         .arg(clientId, limit)
         .additional("endpoint=/api/data")
         .send();

logClient.log(LogLevel.ERROR)
         .code("AUTH_003")
         .arg(userId, requestId)
         .exception(ex)
         .send();
```

| Метод | Описание |
|---|---|
| `.code(String)` | Код лог-записи (обязателен) |
| `.arg(Object...)` | Параметры шаблона |
| `.exception(Throwable)` | Добавить stack trace исключения |
| `.additional(String)` | Произвольный текст для контекста |
| `.send()` | Поставить запись в очередь |

## Принцип работы

1. `info()` / `log().send()` кладёт `LogEntryRequest` во внутреннюю `LinkedBlockingQueue`.
2. Фоновый поток (`log-client-worker`) каждые `flush-interval-ms` мс извлекает до `batch-size` записей и передаёт их в `LogSink.send()`.
3. При переполнении очереди запись отбрасывается; в лог пишется предупреждение (не чаще одного раза на каждые 1000 отброшенных).
4. При завершении приложения (`close()`) — ожидает завершения воркера (до 3 с) и сбрасывает оставшееся в очереди.

## Отключение

```yaml
log-master:
  client:
    enabled: false
```

Бин `LogClient` не будет создан. Удобно для тестовых окружений.
