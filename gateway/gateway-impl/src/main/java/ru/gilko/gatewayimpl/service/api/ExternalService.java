package ru.gilko.gatewayimpl.service.api;

import org.springframework.data.domain.Page;
import ru.gilko.carsapi.dto.CarOutDto;
import ru.gilko.gatewayapi.dto.car.CarDto;
import ru.gilko.gatewayimpl.wrapper.FallbackWrapper;
import ru.gilko.paymentapi.dto.PaymentOutDto;
import ru.gilko.rentalapi.dto.RentalInDto;
import ru.gilko.rentalapi.dto.RentalOutDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExternalService {
    Page<CarDto> getCars(boolean showAll, int page, int size);

    CarOutDto getCar(UUID carUid);

    boolean changeCarAvailability(UUID carId, boolean availability);

    FallbackWrapper<PaymentOutDto> createPayment(int totalPrice);

    FallbackWrapper<RentalOutDto> createRental(String username, RentalInDto rentalInDto);

    boolean cancelRental(String username, UUID rentalUid);

    List<RentalOutDto> getRentals(String username);

    FallbackWrapper<RentalOutDto> getRental(String username, UUID rentalUid);

    boolean cancelPayment(UUID paymentsUid);

    Map<UUID, PaymentOutDto> getPayments(List<UUID> pyamentsUids);

    Map<UUID, CarOutDto> getCars(List<UUID> carUids);

    PaymentOutDto getPayment(UUID paymentUid);

    boolean finishRental(UUID rentalUid, String username);
}
