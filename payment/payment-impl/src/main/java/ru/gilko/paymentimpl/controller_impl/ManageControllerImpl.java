package ru.gilko.paymentimpl.controller_impl;

import org.springframework.http.ResponseEntity;
import ru.gilko.paymentapi.controller.ManageController;

public class ManageControllerImpl implements ManageController {
    @Override
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }
}
