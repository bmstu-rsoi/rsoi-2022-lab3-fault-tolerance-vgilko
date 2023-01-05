package ru.gilko.paymentimpl.controller_impl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.gilko.paymentapi.controller.ManageController;

@RestController
public class ManageControllerImpl implements ManageController {
    @Override
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }
}
