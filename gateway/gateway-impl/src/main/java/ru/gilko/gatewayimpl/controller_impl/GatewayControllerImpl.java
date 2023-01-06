package ru.gilko.gatewayimpl.controller_impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import ru.gilko.gatewayapi.controller.GatewayController;
import ru.gilko.gatewayapi.dto.CarBookDto;
import ru.gilko.gatewayapi.dto.car.CarDto;
import ru.gilko.gatewayapi.dto.rental.RentalCreationOutDto;
import ru.gilko.gatewayapi.dto.rental.RentalDto;
import ru.gilko.gatewayapi.dto.wrapper.PageableCollectionOutDto;
import ru.gilko.gatewayapi.exception.ServiceUnavailableException;
import ru.gilko.gatewayimpl.service.api.GatewayService;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@Slf4j
public class GatewayControllerImpl implements GatewayController {
    private final GatewayService gatewayService;

    public GatewayControllerImpl(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Override
    public PageableCollectionOutDto<CarDto> getAllCars(boolean showAll, int page, int size) {
        log.info("Request for reading cars. Request params: showAll {}, page {}, size {}", showAll, page, showAll);

        return gatewayService.getAllCars(showAll, page, size);
    }

    @Override
    public ResponseEntity<List<RentalDto>> getRental(String username) {
        log.info("Request for reading all rental info of user {}", username);

        return ResponseEntity.ok(gatewayService.getRental(username));
    }

    @Override
    public ResponseEntity<RentalDto> getRental(String username, UUID rentalUid) {
        log.info("Request for reading user's {} rental {}", username, rentalUid);

        return ResponseEntity.ok(gatewayService.getRental(username, rentalUid));
    }

    @Override
    public ResponseEntity<RentalCreationOutDto> bookCar(String userName, CarBookDto carBookDto) {
        log.info("Request for booking car {} by user {}", carBookDto, userName);

        RentalCreationOutDto rentalCreationOutDto = gatewayService.bookCar(userName, carBookDto);

        return ResponseEntity.ok(rentalCreationOutDto);
    }

    @Override
    public ResponseEntity<?> finishRental(String username, UUID rentalUid) {
        log.info("Request for finishing rental {} of user {}", rentalUid, username);

        gatewayService.finishRental(username, rentalUid);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> cancelRental(String username, UUID rentalUid) {
        log.info("Request for canceling rental {} of user {}", rentalUid, username);

        gatewayService.cancelRental(username, rentalUid);

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<?> serviceUnavailable(RuntimeException e) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.put("Content-type", List.of("application/json"));
        return new ResponseEntity<>(Map.of("message", e.getMessage()), headers, HttpStatus.SERVICE_UNAVAILABLE);
    }
}

