# billing-bot
Бот для биллинговой системы ACP LANBilling

## Зависимости

- Java 17
- Spring Boot 2.6.7
- Gradle 7.4.1
- MySQL 8.0
- Docker Engine 20.10.14 (для запуска интегро-тестов)

## Сборка

- Запуск юнит-тестов: `$ ./gradlew test`
- Запуск интегро-тестов: `$ ./gradlew integrationTest`
- Сборка артефакта: `$ ./gradlew bootJar`
- Запуск артефакта: `$ java -Dspring.config.location=/path/to/application.yml -jar /path/to/billing-bot.jar`


## Настроечные ключи

| Ключ                                                   | Значение по-умолчанию | Описание                                                                                            |
|--------------------------------------------------------|-----------------------|-----------------------------------------------------------------------------------------------------|
| bot.enabled                                            |                       | Включить запуск бота                                                                                |
| bot.token                                              |                       | Токен бота                                                                                          |
| bot.name                                               |                       | Имя бота                                                                                            |
| bot.web-client.scheme                                  | http                  | Схема подключения в URL к биллингу                                                                  |
| bot.web-client.host                                    |                       | IP адрес биллинга                                                                                   |
| bot.web-client.port                                    |                       | Порт биллинга                                                                                       |
| bot.web-client.conn-timeout-millis                     | 5000                  | Таймаут при установке соединения к биллингу                                                         |
| bot.web-client.read-timeout-millis                     | 5000                  | Таймаут при чтении по сети из биллингa                                                              |
| bot.web-client.write-timeout-millis                    | 5000                  | Таймаут при записи по сети в биллинг                                                                |
| bot.web-client.manager.login                           |                       | Логин менеджера в биллинге                                                                          |
| bot.web-client.manager.password                        |                       | Пароль менеджера в биллинге                                                                         |
| bot.payment-notification.five-days-send-rule           | "0 0 13 L-5 * *"      | Выражение в формате cron для запуска рассылки напоминаний за 5 дней до конца месяца                 |
| bot.payment-notification.five-days-delete-rule         | "0 0 21 L-4 * *"      | Выражение в формате cron для удаления напоминаний, которые были разосланы за 5 дней до конца месяца |
| bot.payment-notification.one-day-send-rule             | "0 0 13 L-1 * *"      | Выражение в формате cron для запуска рассылки напоминаний за 1 день до конца месяца                 |
| bot.payment-notification.one-day-delete-rule           | "0 0 21 L * *"        | Выражение в формате cron для удаления напоминаний, которые были разосланы за 1 день до конца месяца |
| bot.payment-notification.billing-request-delay-seconds | 1                     | Задержка в секундах при обращении в биллинг при формировании напоминания                            |
| bot.error-group-notification.enabled                   | false                 | Включена ли отправка сообщения об ошибке в группу с ошибками                                        |
| bot.error-group-notification.chat-id                   |                       | ID группы с ошибками                                                                                |
| bot.cache.sbss-knowledge-expired-hours                 | 24                    | Время жизни кэша для тарифов из базы знаний                                                         |
