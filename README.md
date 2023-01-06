# Лабораторная работа #3

![GitHub Classroom Workflow](../../workflows/GitHub%20Classroom%20Workflow/badge.svg?branch=master)

## Fault Tolerance

### Формулировка

На базе [Лабораторной работы #2](https://github.com/bmstu-rsoi/lab2-template) реализовать механизмы, увеличивающие
отказоустойчивость системы.

### Требования

1. На Gateway Service для _всех операций_ чтения реализовать паттерн Circuit Breaker. Накапливать статистику в памяти, и
   если система не ответила N раз, то в N + 1 раз вместо запроса сразу отдавать fallback. Через небольшой timeout
   выполнить запрос к реальной системе, чтобы проверить ее состояние.
2. На каждом сервисе сделать специальный endpoint `GET /manage/health`, отдающий 200 ОК, он будет использоваться для
   проверки доступности сервиса (в [Github Actions](.github/workflows/classroom.yml) в скрипте проверки готовности всех
   сервисов [wait-script.sh](scripts/wait-script.sh) и в тестах [test-script.sh](scripts/test-script.sh)).
   ```shell
   "$path"/wait-for.sh -t 120 "http://localhost:$port/manage/health" -- echo "Host localhost:$port is active"
   ```
4. В случае недоступности данных из некритичного источника (не основного), возвращается fallback-ответ. В зависимости от
   ситуации, это может быть:
    * пустой объект или массив;
    * объект, с заполненным полем (`uid` или подобным), по которому идет связь с другой системой;
    * default строка (если при этом не меняется тип переменной).
5. В задании описаны две операции, изменяющие состояния нескольких систем. В случае недоступности одной из систем,
   участвующих в этой операции, выполнить:
    1. откат всей операции;
    2. возвращать пользователю ответ об успешном завершении операции, а на Gateway Service поставить этот запрос в
       очередь для повторного выполнения.
6. Для автоматических прогонов тестов в файле [autograding.json](.github/classroom/autograding.json)
   и [classroom.yml](.github/workflows/classroom.yml) заменить `<variant>` на ваш вариант.
7. В [docker-compose.yml](docker-compose.yml) прописать сборку и запуск docker контейнеров.
8. Код хранить на Github, для сборки использовать Github Actions.
9. Каждый сервис должен быть завернут в docker.
10. В classroom.yml дописать шаги на сборку, прогон unit-тестов.

### Пояснения

1. Для локальной разработки можно использовать Postgres в docker.
2. Схема взаимодействия сервисов остается как в [Лабораторной работы #2](https://github.com/bmstu-rsoi/lab2-template).
3. Для реализации очереди можно использовать language native реализацию (например, BlockingQueue для Java), либо
   какую-то готовую реализацию типа RabbitMQ, Redis, ZeroMQ и т.п. Крайне нежелательно использовать реляционную базу
   данных как средство эмуляции очереди.
4. Можно использовать внешнюю очередь или запускать ее в docker.
5. Для проверки отказоустойчивости используется остановка и запуск контейнеров docker, это делает
   скрипт [test-script.sh](scripts/test-script.sh). Скрипт нужно запускать из корня проекта, т.к. он обращается к папке
   postman по вариантам.
   ```shell
   # запуск тестового сценария:
   # * <variant> – номер варианта (v1 | v2 | v3 | v4 )
   # * <service> – имя сервиса в Docker Compose
   # * <port>    – порт, на котором запущен сервис
   $ scripts/test-script.sh <variant> <service> <port>
   ```

### Прием задания

1. При получении задания у вас создается fork этого репозитория для вашего пользователя.
2. После того как все тесты успешно завершатся, в Github Classroom на Dashboard будет отмечено успешное выполнение
   тестов.

### Варианты заданий

Распределение вариантов заданий аналогично [ЛР #2](https://github.com/bmstu-rsoi/lab2-template).

1. [Flight Booking System](info/variants/v1/README.md)
2. [Hotels Booking System](info/variants/v2/README.md)
3. [Car Rental System](info/variants/v3/README.md)
4. [Library System](info/variants/v4/README.md)
