## Library System

Система позволяет пользователю найти интересующую книгу и взять ее в библиотеке. Если у пользователя на руках есть уже N
книг, то он не может взять новую, пока не сдал старые. Если пользователь возвращает книги в хорошем состоянии и сдает их
в срок, то максимальное количество книг у него на руках увеличивается.

##### Деградация функциональности

Если метод требует получения данных из нескольких источников, то в случае недоступности одного _не критичного_
источника, то недостающие данные возвращаются как некоторый fallback ответ, а остальные данные подставляются из
успешного запроса.

Например, методы `GET /api/v1/libraries` и `GET /api/v1/libraries/{libraryUid}/books` в случае недоступности Library
Service должен вернуть 500 ошибку, т.к. данные, получаемые из этого сервиса критичные. Аналогично для
метода `GET /api/v1/rating` из сервиса Rating Service.

Для метода `GET /api/v1/reservations` в случае недоступности Reservation Service запрос должен вернуть 500 ошибку, а в
случае недоступности Library Service, поля `book` и `library` должно содержать только uid записи (`bookUid`
и `libraryUid` соответственно).

##### Взять книгу в библиотеке

1. Запрос к Library Service и Rating Service для проверки, что пользователь может взять книгу. Если один из этих
   сервисов недоступен, то запрос завершается с ошибкой.
1. Выполняется запрос к Reservation System для создания записи о получении книги на руки. Если сервис недоступен, то
   запрос завершается с ошибкой.
1. Выполняется запрос в Library Service для отметки, что книга была взята (уменьшение счетчика доступных
   книг `available_count`).
1. Если запрос к Library Service завершился неудачей (500 ошибка или сервис недоступен), то выполняется откат резерва
   книги в Reservation Service.

##### Вернуть книгу в библиотеку

1. Выполняется запрос к Rental Service для установки статуса возврата `RETURNED` или `EXPIRED` в соответствии с
   описанными в [ЛР #2 условиями](https://github.com/bmstu-rsoi/lab2-template/blob/master/v4/README.md).
1. После этого выполняется запрос к Library Service для увеличения количества доступных книг (поле `available_count`).
   Если этот сервис недоступен, то на Gateway Service запрос поставить в очередь и повторять пока он не завершится
   успехом (timeout 10 секунд), потом перейти к следующему шагу.
1. Выполнить запрос к Rating Service для изменения рейтинга пользователя. Если сервис недоступен, то аналогично
   предыдущему шагу, на Gateway Service запрос ставится в очередь и повторяется пока не завершится успехом (timeout 10
   секунд), при этом пользователю отдается информация об успешном завершении всей операции.

Описание в формате [Open API](%5Binst%5D%5Bv4%5D%20Library%20System.yml).

### Данные для тестов

В тестовом сценарии выключается _Rating Service_, необходимо в переменную `serviceName` в
в [[classroom.yml](../../../.github/workflows/classroom.yml)] прописать его название в Heroku.

Создать данные для тестов:

```yaml
library:
  – id: 1
    library_uid: "83575e12-7ce0-48ee-9931-51919ff3c9ee"
    name: "Библиотека имени 7 Непьющих"
    city: "Москва"
    address: "2-я Бауманская ул., д.5, стр.1"

books:
  - id: 1
    book_uid: "f7cdc58f-2caf-4b15-9727-f89dcc629b27",
    name: "Краткий курс C++ в 7 томах",
    author: "Бьерн Страуструп",
    genre: "Научная фантастика",
    condition: "EXCELLENT",

library_books:
  - book_id: 1
    library_id: 1
    avaiblable_count: 1
```