package ru.gilko.rentalimpl.controller_impl;

import org.springframework.http.ResponseEntity;
import ru.gilko.rentalapi.controller.ManageController;

public class ManageControllerImpl implements ManageController {
    @Override
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }
}
