package ru.gilko.gatewayimpl.service.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.gilko.carsapi.dto.CarOutDto;
import ru.gilko.carsapi.feign.CarFeign;
import ru.gilko.gatewayapi.dto.car.CarDto;
import ru.gilko.gatewayapi.exception.InternalServiceException;
import ru.gilko.gatewayapi.exception.NoSuchEntityException;
import ru.gilko.gatewayimpl.service.api.ExternalService;
import ru.gilko.gatewayimpl.wrapper.FallbackWrapper;
import ru.gilko.paymentapi.dto.PaymentOutDto;
import ru.gilko.paymentapi.feign.PaymentFeign;
import ru.gilko.rentalapi.dto.RentalInDto;
import ru.gilko.rentalapi.dto.RentalOutDto;
import ru.gilko.rentalapi.feign.RentalFeign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExternalServiceImpl implements ExternalService {
    private final CarFeign carFeign;
    private final RentalFeign rentalFeign;
    private final PaymentFeign paymentFeign;

    private final ModelMapper modelMapper;


    public ExternalServiceImpl(CarFeign carFeign, RentalFeign rentalFeign, PaymentFeign paymentFeign, ModelMapper modelMapper) {
        this.carFeign = carFeign;
        this.rentalFeign = rentalFeign;
        this.paymentFeign = paymentFeign;
        this.modelMapper = modelMapper;
    }


    @Override
    public Page<CarDto> getCars(boolean showAll, int page, int size) {
        try {
            Page<CarOutDto> carOutDtos = carFeign.getCars(showAll, page, size);

            log.info("Received {} entities from car service", carOutDtos.getTotalElements());

            return mapToCarDto(carOutDtos);
        } catch (FeignException e) {
            log.error("Exception was occurred when getting cars. Exception: {}", e.getMessage());
            throw new InternalServiceException("Unable to get info from Car service.");
        }
    }

    @Override
    public Map<UUID, CarOutDto> getCars(List<UUID> carUids) {
        try {
            return carFeign.getCars(carUids)
                    .stream()
                    .collect(Collectors.toMap(CarOutDto::getCarUid, Function.identity()));
        } catch (FeignException e) {
            log.error("Exception was occurred during calling car service. Exception: {}", e.getMessage());

            return buildIdFilledObjects(carUids, this::fillOnlyIdInCarOutDto);
        }
    }

    @Override
    public CarOutDto getCar(UUID carUid) {
        try {
            return carFeign.getCar(carUid);
        } catch (FeignException.NotFound e) {
            log.error("404 during getting car with uuid [{}]. Exception: {}", carUid, e.getMessage());

            throw e;
        } catch (FeignException e) {
            log.error("Exception was occurred during calling car service. Exception: {}", e.getMessage());

            CarOutDto car = new CarOutDto();
            car.setCarUid(carUid);

            return car;
        }
    }

    @Override
    public boolean changeCarAvailability(UUID carId, boolean availability) {
        try {
            carFeign.changeAvailability(carId, availability);

            log.info("Changed car [{}] availability to [{}]", carId, availability);

            return true;
        } catch (FeignException.NotFound e) {
            log.error("Trying to change availability for non existing car {}", carId);
            throw new NoSuchEntityException("There is no car with id = %s".formatted(carId));
        } catch (FeignException e) {
            log.error("Exception was occurred when changing availability of car {}. Exception: {}", carId, e.getMessage());

            return false;
        }
    }

    @Override
    public FallbackWrapper<PaymentOutDto> createPayment(int totalPrice) {
        try {
            return new FallbackWrapper<>(paymentFeign.createPayment(totalPrice), true);
        } catch (FeignException e) {
            log.error("Exception was occurred during creating payment for price [{}]. Exception: {}",
                    totalPrice, e.getMessage());

            return new FallbackWrapper<>(null, false);
        }
    }

    @Override
    public FallbackWrapper<RentalOutDto> createRental(String username, RentalInDto rentalInDto) {
        try {
            return new FallbackWrapper<>(rentalFeign.createRental(username, rentalInDto), true);
        } catch (FeignException e) {
            log.error("Exception was occurred during creating rental for user [{}], rental [{}]. Exception: {}",
                    username, rentalInDto, e.getMessage());

            return new FallbackWrapper<>(null, false);
        }
    }

    @Override
    public boolean cancelRental(String username, UUID rentalUid) {
        try {
            rentalFeign.cancelRental(rentalUid, username);

            return true;
        } catch (FeignException.NotFound e) {
            log.error("404 during cancelling rental [{}] of user [{}]. Exception: {}", rentalUid, username, e.getMessage());

            throw new NoSuchEntityException(e.getMessage());
        } catch (FeignException e) {
            log.error("Exception was occurred during cancelling rental [{}] of user [{}]. Exception: {}",
                    rentalUid, username, e.getMessage());

            return false;
        }
    }

    @Override
    public List<RentalOutDto> getRentals(String username) {
        try {
            return rentalFeign.getRentals(username);
        } catch (FeignException.NotFound e) {
            log.error("404 during getting user [{}] rentals. Exception: {}", username, e.getMessage());
            throw e;
        } catch (FeignException e) {
            log.error("Exception was occurred during calling rental service. Exception: {}", e.getMessage());

            throw new InternalServiceException("Unable to get info from Rental service.");
        }
    }

    @Override
    public FallbackWrapper<RentalOutDto> getRental(String username, UUID rentalUid) {
        try {
            RentalOutDto rental = rentalFeign.getRental(rentalUid, username);
            log.info("Get rental form rental service: {}", rental);

            return new FallbackWrapper<>(rental, true);
        } catch (FeignException.NotFound e) {
            log.error("404 during getting user [{}] rental with id [{}]. Exception: {}", username, rentalUid, e.getMessage());
            throw new NoSuchEntityException(e.getMessage());
        } catch (FeignException e) {
            log.error("Exception was occurred during calling rental service. Exception: {}", e.getMessage());

            return new FallbackWrapper<>(null, false);
        }
    }

    @Override
    public boolean cancelPayment(UUID paymentsUid) {
        try {
            paymentFeign.cancelPayment(paymentsUid);

            return true;
        } catch (FeignException.NotFound e) {
            log.error("404 during cancelling payment with id [{}]. Exception: {}", paymentsUid, e.getMessage());

            throw new NoSuchEntityException(e.getMessage());
        } catch (FeignException e) {
            log.error("Exception was occurred during calling payment service. Exception: {}", e.getMessage());

            return false;
        }
    }

    @Override
    public Map<UUID, PaymentOutDto> getPayments(List<UUID> paymentsUids) {
        try {
            return paymentFeign.getPayments(paymentsUids)
                    .stream()
                    .collect(Collectors.toMap(PaymentOutDto::getPaymentUid, Function.identity()));
        } catch (FeignException e) {
            log.error("Exception was occurred during calling payment service. Exception: {}", e.getMessage());

            return buildIdFilledObjects(paymentsUids, this::fillOnlyIdInPaymentOutDto);
        }
    }

    @Override
    public PaymentOutDto getPayment(UUID paymentUid) {
        try {
            return paymentFeign.getPayment(paymentUid);
        } catch (FeignException.NotFound e) {
            log.error("404 during getting payment with uuid [{}]. Exception: {}", paymentUid, e.getMessage());

            throw e;
        } catch (FeignException e) {
            log.error("Exception was occurred during calling payment service. Exception: {}", e.getMessage());

            PaymentOutDto payment = new PaymentOutDto();
            payment.setPaymentUid(paymentUid);

            return payment;
        }
    }

    @Override
    public boolean finishRental(UUID rentalUid, String username) {
        try {
            rentalFeign.finishRental(rentalUid, username);
            return true;
        } catch (FeignException.NotFound e) {
            log.error("404 during finishing rental with uuid [{}]. Exception: {}", rentalUid, e.getMessage());

            throw new NoSuchEntityException(e.getMessage());
        } catch (FeignException e) {
            log.error("Exception was occurred during calling rental service. Exception: {}", e.getMessage());

            return false;
        }
    }

    private Page<CarDto> mapToCarDto(Page<CarOutDto> page) {
        return page.map(car -> modelMapper.map(car, CarDto.class));
    }

    private PaymentOutDto fillOnlyIdInPaymentOutDto(UUID uuid) {
        PaymentOutDto payment = new PaymentOutDto();
        payment.setPaymentUid(uuid);
        return payment;
    }


    private CarOutDto fillOnlyIdInCarOutDto(UUID uuid) {
        CarOutDto car = new CarOutDto();
        car.setCarUid(uuid);
        return car;
    }

    private <R> Map<UUID, R> buildIdFilledObjects(List<UUID> carUids, Function<UUID, R> mapperFunction) {
        HashMap<UUID, R> result = new HashMap<>();

        carUids.forEach(uuid ->
                result.put(uuid, mapperFunction.apply(uuid)));

        return result;
    }
}
