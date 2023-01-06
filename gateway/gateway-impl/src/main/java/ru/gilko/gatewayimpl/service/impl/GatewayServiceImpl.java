package ru.gilko.gatewayimpl.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.gilko.carsapi.dto.CarOutDto;
import ru.gilko.gatewayapi.dto.CarBookDto;
import ru.gilko.gatewayapi.dto.car.CarBaseDto;
import ru.gilko.gatewayapi.dto.car.CarDto;
import ru.gilko.gatewayapi.dto.payment.PaymentDto;
import ru.gilko.gatewayapi.dto.rental.RentalCreationOutDto;
import ru.gilko.gatewayapi.dto.rental.RentalDto;
import ru.gilko.gatewayapi.dto.wrapper.PageableCollectionOutDto;
import ru.gilko.gatewayapi.exception.InternalServiceException;
import ru.gilko.gatewayapi.exception.InvalidOperationException;
import ru.gilko.gatewayapi.exception.ServiceUnavailableException;
import ru.gilko.gatewayimpl.service.api.ExternalService;
import ru.gilko.gatewayimpl.service.api.GatewayService;
import ru.gilko.gatewayimpl.wrapper.FallbackWrapper;
import ru.gilko.paymentapi.dto.PaymentOutDto;
import ru.gilko.rentalapi.dto.RentalInDto;
import ru.gilko.rentalapi.dto.RentalOutDto;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
@Slf4j
public class GatewayServiceImpl implements GatewayService {

    private final ExternalService externalService;

    private final Trying trying;

    private final ModelMapper modelMapper;

    public GatewayServiceImpl(ExternalService externalService, Trying trying, ModelMapper modelMapper) {
        this.externalService = externalService;
        this.trying = trying;
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void startTrying() {

        Thread thread = new Thread(() -> {
            try {
                trying.resendRequests();
            } catch (InterruptedException e) {
            }
        });
        thread.start();

        log.debug("PostConstruct was worked");
    }

    @Override
    public PageableCollectionOutDto<CarDto> getAllCars(boolean showAll, int page, int size) {
        return buildPageCollectionOutDto(externalService.getCars(showAll, page, size));
    }

    @Override
    public List<RentalDto> getRental(String username) {
        List<RentalOutDto> rentals = externalService.getRentals(username);

        List<UUID> paymentsUids = new LinkedList<>();
        List<UUID> carUids = new LinkedList<>();
        rentals.forEach(rentalOutDto -> {
            paymentsUids.add(rentalOutDto.getPaymentUid());
            carUids.add(rentalOutDto.getCarUid());
        });

        Map<UUID, PaymentOutDto> payments = externalService.getPayments(paymentsUids);
        Map<UUID, CarOutDto> cars = externalService.getCars(carUids);

        return rentals.stream()
                .map(rentalOutDto -> buildOutDto(rentalOutDto, payments, cars))
                .toList();
    }

    @Override
    public RentalDto getRental(String username, UUID rentalUid) {
        FallbackWrapper<RentalOutDto> rental = externalService.getRental(username, rentalUid);
        if (!rental.isValidResponse()) {
            throw new InternalServiceException("Unable to get info from Rental service.");
        }

        CarOutDto car = externalService.getCar(rental.getValue().getCarUid());
        PaymentOutDto payment = externalService.getPayment(rental.getValue().getPaymentUid());

        RentalDto mappedRental = modelMapper.map(rental.getValue(), RentalDto.class);
        mappedRental.setCar(modelMapper.map(car, CarBaseDto.class));
        mappedRental.setPayment(modelMapper.map(payment, PaymentDto.class));

        log.debug("RentalDto: {}", mappedRental);

        return mappedRental;
    }

    @Override
    public RentalCreationOutDto bookCar(String userName, CarBookDto carBookDto) {
        CarOutDto car = externalService.getCar(carBookDto.getCarUid());

        if (!car.isAvailable()) {
            log.error("Trying to book not available car {}", car.getCarUid());
            throw new InvalidOperationException("Car %s is not available".formatted(car.getCarUid()));
        }

        changeCarAvailabilitySafety(car.getCarUid(), false);

        FallbackWrapper<PaymentOutDto> payment = createPayment(carBookDto, car.getPrice());
        if (!payment.isValidResponse()) {
            log.error("Unable to create payment for user [{}] order {}. Rollback changes", userName, carBookDto);

            changeCarAvailabilitySafety(car.getCarUid(), true);

            throw new ServiceUnavailableException("Payment Service unavailable");
        }

        FallbackWrapper<RentalOutDto> rental = createRental(userName, carBookDto, payment.getValue());
        if (!rental.isValidResponse()) {
            log.error("Unable to create rental for user [{}] order [{}]. Rollback changes", userName, carBookDto);

            changeCarAvailabilitySafety(car.getCarUid(), true);
            externalService.cancelPayment(payment.getValue().getPaymentUid());

            throw new ServiceUnavailableException("Rental Service unavailable");
        }

        RentalCreationOutDto rentalCreationOutDto = modelMapper.map(rental.getValue(), RentalCreationOutDto.class);
        rentalCreationOutDto.setPayment(modelMapper.map(payment.getValue(), PaymentDto.class));
        rentalCreationOutDto.setCarUid(carBookDto.getCarUid());

        log.debug("RentalDto after booking car {}", rentalCreationOutDto);

        return rentalCreationOutDto;

    }

    private FallbackWrapper<PaymentOutDto> createPayment(CarBookDto carBookDto, int carRentalPrice) {
        int amountRentalDays = (int) calculateAmountRentalDays(carBookDto);
        int totalPrice = amountRentalDays * carRentalPrice;

        return externalService.createPayment(totalPrice);
    }

    private FallbackWrapper<RentalOutDto> createRental(String username, CarBookDto carBookDto, PaymentOutDto payment) {
        RentalInDto rentalInDto = new RentalInDto(
                carBookDto.getCarUid(), payment.getPaymentUid(), carBookDto.getDateFrom(), carBookDto.getDateTo());

        return externalService.createRental(username, rentalInDto);
    }

    @Override
    public boolean finishRental(String username, UUID rentalUid) {
        FallbackWrapper<RentalOutDto> rental = externalService.getRental(username, rentalUid);
        if (rental.isValidResponse()) {
            UUID carUid = rental.getValue().getCarUid();
            changeCarAvailabilitySafety(carUid, true);

            if (!externalService.finishRental(rentalUid, username)) {
                log.error("Unable to finish rental [{}] of user [{}]. Request will be resend", rentalUid, username);
                trying.addRequest(buildHash(username, rentalUid),
                        () -> externalService.finishRental(rentalUid, username));
            }
        } else {
            log.error("Unable to get rental [{}] of user [{}] from rental service. Request will be resend",
                    rentalUid, username);
            trying.addRequest(buildHash(username, rentalUid),
                    () -> this.finishRental(username, rentalUid));
            return false;
        }

        return true;
    }

    private void changeCarAvailabilitySafety(UUID carUid, boolean availability) {
        boolean isChanged = externalService.changeCarAvailability(carUid, availability);

        if (!isChanged) {
            log.error("Unable to change car [{}] availability to [{}]. Request will be resend",
                    carUid, availability);

            trying.addRequest(buildHash(carUid, availability),
                    () -> externalService.changeCarAvailability(carUid, availability));
        }
    }

    private Integer buildHash(String username, UUID rentalUid) {
        return (username + rentalUid).hashCode();
    }

    private Integer buildHash(UUID carUid, boolean availability) {
        return (carUid.toString() + availability).hashCode();
    }

    @Override
    public boolean cancelRental(String username, UUID rentalUid) {
        FallbackWrapper<RentalOutDto> rental = externalService.getRental(username, rentalUid);
        if (rental.isValidResponse()) {
            changeCarAvailabilitySafety(rental.getValue().getCarUid(), true);

            boolean isRentalCancelled = externalService.cancelRental(username, rentalUid);
            if (!isRentalCancelled) {
                trying.addRequest(buildHash(username, rentalUid),
                        () -> externalService.cancelRental(username, rentalUid));
            }

            boolean isPaymentCancelled = externalService.cancelPayment(rental.getValue().getPaymentUid());
            if (!isPaymentCancelled) {
                trying.addRequest(buildHash(username, rentalUid),
                        () -> externalService.cancelPayment(rental.getValue().getPaymentUid()));
            }

        } else {
            trying.addRequest(buildHash(username, rentalUid),
                    () -> this.cancelRental(username, rentalUid));
            return false;
        }

        return true;
    }


    private long calculateAmountRentalDays(CarBookDto carBookDto) {
        long totalRentalDays = DAYS.between(carBookDto.getDateFrom(), carBookDto.getDateTo());

        if (totalRentalDays < 0) {
            log.error("Trying to create rental with invalid dates DateFrom {}, DateTo {}",
                    carBookDto.getDateFrom(),
                    carBookDto.getDateTo());
            throw new InvalidOperationException("Invalid car rental dates. DateTo should be after DateFrom");
        }

        return totalRentalDays;
    }

    private RentalDto buildOutDto(RentalOutDto rentalOutDto, Map<UUID, PaymentOutDto> payments, Map<UUID, CarOutDto> cars) {
        RentalDto rental = modelMapper.map(rentalOutDto, RentalDto.class);

        PaymentOutDto payment = payments.get(rentalOutDto.getPaymentUid());
        PaymentDto paymentDto = modelMapper.map(payment, PaymentDto.class);

        CarOutDto car = cars.get(rentalOutDto.getCarUid());
        CarDto carDto = modelMapper.map(car, CarDto.class);

        rental.setPayment(paymentDto);
        rental.setCar(carDto);

        return rental;
    }


    private <T> PageableCollectionOutDto<T> buildPageCollectionOutDto(Page<T> page) {
        return new PageableCollectionOutDto<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalPages());
    }
}
