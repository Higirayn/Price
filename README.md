# Price Service - Высоконагруженный веб-сервис для обработки цен товаров

## Описание

Веб-сервис для приема и обновления данных о ценах товаров от различных производителей с расчетом средней цены в режиме реального времени. Приложение демонстрирует высокую степень параллелизма и возможность обработки множества сообщений одновременно.

## Архитектура

### Основные компоненты:

1. **DatabaseService** - сервис для работы с PostgreSQL через JDBC
2. **PriceProcessingService** - сервис для параллельной обработки обновлений цен
3. **PriceUpdateServlet** - HTTP сервлет для приема обновлений цен
4. **AveragePriceServlet** - HTTP сервлет для получения средних цен
5. **Jetty Server** - встроенный веб-сервер

### Технологии:

- **Java 11+**
- **Jetty** - встроенный HTTP сервер
- **PostgreSQL** - база данных
- **HikariCP** - пул соединений
- **Jackson** - JSON обработка
- **SLF4J + Logback** - логирование
- **Gradle** - сборка проекта

## Требования

- Java 11 или выше
- PostgreSQL 12 или выше
- Gradle 7.0 или выше

## Установка и настройка

### 1. Настройка базы данных

Создайте базу данных PostgreSQL:

```sql
CREATE DATABASE price_service;
CREATE USER price_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE price_service TO price_user;
```

### 2. Настройка подключения к БД

Отредактируйте параметры подключения в `DatabaseService.java`:

```java
config.setJdbcUrl("jdbc:postgresql://localhost:5432/price_service");
config.setUsername("price_user");
config.setPassword("your_password");
```

### 3. Сборка проекта

```bash
./gradlew build
```

### 4. Запуск приложения

```bash
./gradlew run
```

Приложение запустится на порту 8080.

## API Endpoints

### 1. Обновление цен товаров

**POST** `/api/prices/update`

**Content-Type:** `application/json`

**Пример запроса:**
```json
[
    {
        "product_id": 1,
        "manufacturer_name": "Производитель A",
        "price": 100.50
    },
    {
        "product_id": 2,
        "manufacturer_name": "Производитель B",
        "price": 150.75
    },
    {
        "product_id": 1,
        "manufacturer_name": "Производитель C",
        "price": 200.00
    }
]
```

**Ответ:**
```json
{
    "status": "success",
    "message": "Price updates accepted for processing",
    "data": 3
}
```

### 2. Получение всех средних цен

**GET** `/api/prices/average`

**Ответ:**
```json
{
    "status": "success",
    "message": "Average prices retrieved",
    "data": [
        {
            "productId": 1,
            "averagePrice": 150.25,
            "offerCount": 2
        },
        {
            "productId": 2,
            "averagePrice": 150.75,
            "offerCount": 1
        }
    ]
}
```

### 3. Получение средней цены конкретного товара

**GET** `/api/prices/average/{productId}`

**Пример:** `GET /api/prices/average/1`

**Ответ:**
```json
{
    "status": "success",
    "message": "Average price retrieved",
    "data": {
        "productId": 1,
        "averagePrice": 150.25,
        "offerCount": 2
    }
}
```

## Особенности реализации

### Параллельная обработка

- Использование `ExecutorService` с пулом потоков размером с количество доступных процессоров
- `CompletableFuture` для асинхронной обработки
- `ReadWriteLock` для синхронизации доступа к базе данных

### Синхронизация данных

- Транзакционность при обновлении цен
- Использование `ON CONFLICT` для upsert операций
- Автоматический пересчет средних цен при каждом обновлении

### Обработка ошибок

- Валидация входных данных
- Обработка SQL исключений
- Graceful shutdown приложения

### Мониторинг

- Подробное логирование всех операций
- Отслеживание производительности
- Логирование ошибок и исключений

## Тестирование

### Запуск тестов

```bash
./gradlew test
```

### Тестирование API

Пример с использованием curl:

```bash
# Обновление цен
curl -X POST http://localhost:8080/api/prices/update \
  -H "Content-Type: application/json" \
  -d '[
    {"product_id": 1, "manufacturer_name": "Производитель A", "price": 100},
    {"product_id": 2, "manufacturer_name": "Производитель B", "price": 150},
    {"product_id": 1, "manufacturer_name": "Производитель C", "price": 200}
  ]'

# Получение всех средних цен
curl http://localhost:8080/api/prices/average

# Получение средней цены товара с ID 1
curl http://localhost:8080/api/prices/average/1
```

## Альтернативные архитектурные решения

### 1. Apache Kafka + Stream Processing

Для еще более высокой производительности можно использовать:
- **Apache Kafka** для очередей сообщений
- **Kafka Streams** или **Apache Flink** для потоковой обработки
- **Apache Cassandra** для хранения данных

### 2. Микросервисная архитектура

- Разделение на отдельные сервисы: Price Ingestion, Price Calculation, Price Query
- Использование **Spring Boot** для каждого сервиса
- **Redis** для кэширования средних цен
- **MongoDB** для хранения исторических данных

### 3. Event Sourcing

- Сохранение всех событий изменения цен
- Вычисление средних цен из событий
- Использование **EventStore** или **Apache Kafka** для хранения событий

## Производительность

Приложение оптимизировано для высокой нагрузки:

- Пул соединений HikariCP (20 соединений)
- Параллельная обработка обновлений
- Индексы в базе данных
- Асинхронные ответы API

## Логирование

Логи сохраняются в:
- Консоль (для разработки)
- Файл `logs/price-service.log` (с ротацией по дням)

Уровни логирования:
- `INFO` - основная информация о работе приложения
- `WARN` - предупреждения
- `ERROR` - ошибки и исключения
- `DEBUG` - детальная отладочная информация 