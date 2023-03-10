## Car Rental System

Система предоставляет пользователю возможность забронировать автомобиль на выбранные даты.

Описание в формате [OpenAPI](%5Binst%5D%5Bv3%5D%20Car%20Rental%20System.yml).

##### Деградация функциональности

Если метод требует получения данных из нескольких источников, то в случае недоступности одного _не критичного_
источника, то недостающие данные возвращаются как некоторый fallback ответ, а остальные данные подставляются из
успешного запроса.

Например, метод `GET /api/v1/cars` в случае недоступности Car Service должен вернуть 500 ошибку, т.к. данные, получаемые
из этого сервиса критичные.

Для методов `GET /api/v1/rental` и `GET /api/v1/rental/{{rentalUid}}` в случае недоступности Rental Service запрос
должен вернуть 500 ошибку, а в случае недоступности Payment Service или Car Service, поля `payment` и `car` должны
содержать только uid записи (`paymentUid` и `carUid` соответственно).

##### Бронирование автомобиля

1. Запрос к Car Service для проверки, что такой автомобиль существует и доступен. Если условие выполняется, то
   автомобиль резервируется (`availability = false`). Если Car Service недоступен, то запрос завершается с ошибокой.
1. Выполняется запрос в Rental Service для создания записи о бронировании.
1. Если запрос к Rental Service завершился неудачей (500 ошибка или сервис недоступен), то выполняется откат операции
   резервирования автомобиля в Car Service (`availability = true`)
1. Выполняется запрос к Payment Service для создания оплаты.
1. Если запрос к Payment Service завершился неудачей (500 ошибка или сервис недоступен), то выполняется откат операции
   оплаты в Payment Service и в Car Service снимается резерв с автомобиля (`availability = true`).

##### Отмена бронирования автомобиля

1. Выполняется запрос к Car Service для снятия резерва с автомобиля (`availability = true`).
1. После этого выполняется запрос к Rental Service для установки флага аренды `CANCELED`. Если этот сервис недоступен,
   то на Gateway Service запрос поставить в очередь и повторять пока он не завершится успехом (timeout 10 секунд), потом
   перейти к следующему шагу.
1. Выполнить запрос к Payment Service для отмены оплаты. Если сервис недоступен, то аналогично предыдущему шагу, на
   Gateway Service запрос ставится в очередь и повторяется пока не завершится успехом (timeout 10 секунд), при этом
   пользователю отдается информация об успешном завершении всей операции.

### Данные для тестов

В тестовом сценарии выключается _Payment Service_, необходимо в переменную `serviceName` в
в [[classroom.yml](../../../.github/workflows/classroom.yml)] прописать его название в Heroku.

Создать данные для тестов:

```yaml
cars:
  – id: 1
    car_uid: "109b42f3-198d-4c89-9276-a7520a7120ab"
    brand: "Mercedes Benz"
    model: "GLA 250"
    registration_number: "ЛО777Х799"
    power: 249
    type: "SEDAN"
    price: 3500
    available: true
```