package ru.gilko.gatewayapi.controller_advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.gilko.gatewayapi.exception.InternalServiceException;
import ru.gilko.gatewayapi.exception.InvalidOperationException;
import ru.gilko.gatewayapi.exception.NoSuchEntityException;
import ru.gilko.gatewayapi.exception.ServiceUnavailableException;

@RestControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<?> badRequest(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchEntityException.class)
    public ResponseEntity<?> notFound(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InternalServiceException.class)
    public ResponseEntity<?> internalServiceError(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
