# log-master — Backend

REST API для сбора, хранения и поиска логов в распределённых системах на основе шаблонизации через log-коды.

## Функциональные возможности

- Реестр приложений: регистрация и управление сервисами, чьи логи собирает система
- Управление шаблонами лог-сообщений с поддержкой нескольких языков (ru, en, zh и др.)
- Полнотекстовый поиск по шаблонам — по коду и текстам на всех языках одновременно
- Приём и хранение записей логов с позиционными аргументами и стек-трейсами
- Постраничный поиск записей по приложению, коду шаблона, уровню и временному диапазону
- Агрегированная статистика: распределение по уровням и сервисам, активность за последние дни
- In-memory кэш шаблонов и приложений на базе Caffeine — денормализация `level` без обращения к OpenSearch

## Концепция

Система хранит логи в шаблонизированном виде:

- **LogTemplate** — шаблон сообщения: уникальный `logCode` + тексты на разных языках (`{ "ru": "...", "en": "..." }`)
- **LogEntry** — запись лога: `logCode` + позиционные аргументы + метаданные (timestamp, service, level)
- **Двухэтапный поиск**: полнотекстовый поиск по шаблонам → фильтрация записей по `logCode`

`level` денормализован в `LogEntry` из шаблона при сохранении — для эффективной фильтрации без join-а.

## Механика

### Хранение логов

Запись лога (`LogEntry`) не содержит готового текста сообщения — только `logCode` и массив позиционных аргументов. Текст восстанавливается на стороне клиента путём подстановки аргументов в шаблон нужного языка:

```
template: "Пользователь {0} вошёл с IP {1}"
args:     ["john.doe", "192.168.1.1"]
result:   "Пользователь john.doe вошёл с IP 192.168.1.1"
```

Это позволяет хранить одну запись и отображать её на любом языке без дублирования данных.

### Поиск шаблонов

Полнотекстовый поиск по шаблонам использует `multi_match` с типом **`phrase_prefix`**:

- Поиск ведётся одновременно по полям `code` и `messages.*` (тексты на всех языках)
- Все слова запроса кроме последнего ищутся как точная фраза в заданном порядке
- Последнее слово трактуется как префикс — подходит для автодополнения по мере набора

Пример: запрос `"user log"` найдёт шаблоны, где слово `"user"` стоит перед словами `"log"`, `"logged"`, `"login"` и т.д.

Поле `code` замаплено как `text` (с субполем `.keyword`) — это необходимо для токенизации при `phrase_prefix`. Поле `applicationCode` остаётся `keyword`, поскольку используется только для точечной фильтрации через `term`.

### Кэширование

Шаблоны и приложения кэшируются в памяти через **Caffeine**:

- При старте кэши заполняются из OpenSearch (`@PostConstruct`)
- Периодически обновляются полностью (`@Scheduled`, интервал настраивается)
- При создании/обновлении/удалении — точечная мутация (`cache.put` / `cache.invalidate`) без полной перегрузки

При сохранении нового `LogEntry` `level` резолвится из кэша шаблонов — без дополнительного запроса к OpenSearch.

### OpenSearch индексы

| Индекс | Ключевые поля |
|---|---|
| `applications` | `code` (keyword), `name` (text) |
| `log-templates` | `code` (keyword), `applicationCode` (keyword), `messages` (object), `level` (keyword) |
| `log-entries` | `code` (keyword), `applicationCode` (keyword), `level` (keyword), `created` (date), `args` (object), `additional` (object, not indexed), `exception` (text) |

## Конфигурация (`application.properties`)

```properties
# OpenSearch
opensearch.host=localhost
opensearch.port=9200

# Индексы, создаваемые при старте
opensearch.indices[0]=applications
opensearch.indices[1]=log-templates
opensearch.indices[2]=log-entries

# Caffeine Cache
cache.applications.max-size=1000
cache.applications.refresh-interval-ms=60000
cache.templates.max-size=10000
cache.templates.refresh-interval-ms=60000

# CORS
cors.allowed-origins=http://localhost:5173
```

## API

Базовый путь: `/log-master/api`  
Swagger UI: `http://localhost:8080/log-master/swagger-ui.html`

### Applications — `/api/applications`

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/applications` | Список всех приложений |
| POST | `/api/applications` | Зарегистрировать приложение |
| GET | `/api/applications/{code}` | Получить приложение по коду |
| DELETE | `/api/applications/{code}` | Удалить приложение |

Тело POST:
```json
{ "code": "auth-service", "name": "Сервис аутентификации" }
```

### Log Templates — `/api/templates`

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/templates` | Постраничный список (`appCode`, `page`, `size`) |
| GET | `/api/templates/search` | Полнотекстовый поиск (`appCode`, `q`) |
| POST | `/api/templates` | Создать шаблон |
| PUT | `/api/templates/{logCode}` | Обновить шаблон |
| DELETE | `/api/templates/{logCode}` | Удалить шаблон |

Тело POST/PUT:
```json
{
  "logCode": "AUTH_001",
  "appCode": "auth-service",
  "messages": {
    "ru": "Пользователь {0} вошёл в систему",
    "en": "User {0} logged in",
    "zh": "用户 {0} 已登录"
  }
}
```

### Logs — `/api/logs`

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/logs` | Поиск записей (`appCode`, `logCode`, `level`, `from`, `to`, `page`, `size`) |
| POST | `/api/logs` | Сохранить одну запись лога |
| POST | `/api/logs/batch` | Сохранить пакет записей логов |
| GET | `/api/logs/stats` | Агрегированная статистика (`appCode`) |

Параметры фильтрации времени — ISO 8601, например `2026-05-10T00:00:00Z`.

Тело POST:
```json
{
  "code": "AUTH_001",
  "applicationCode": "auth-service",
  "created": "2026-05-11T10:00:00Z",
  "level": "INFO",
  "args": ["john.doe", "192.168.1.1"],
  "exception": null,
  "additional": "произвольный контекст"
}
```

`additional` принимает любую строку; хранится как `JsonNode` и отдаётся в ответе pretty-printed.

## Структура пакетов

```
ru.loginov.log_master
├── config          # OpenSearch client, Caffeine Cache, CORS, Swagger, IndexInitializer
├── controller      # REST-контроллеры
├── service         # бизнес-логика + кэш-сервисы
├── repository      # запросы к OpenSearch
├── data
│   ├── model       # LogEntry, LogTemplate, Application
│   ├── dto         # Request/Response records
│   └── enums       # LogLevel
└── exception       # NotFoundException, GlobalExceptionHandler, ErrorResponse
```

## Стек

| Компонент | Версия |
|---|---|
| Java | 21 |
| Spring Boot | 4.0 |
| OpenSearch Java Client | 3.x |
| Caffeine Cache | (BOM) |
| Lombok | 1.18 |
| SpringDoc OpenAPI | 2.x |
