package ru.gilko.carsimpl.controller_impl;

import org.springframework.http.ResponseEntity;
import ru.gilko.carsapi.controller.ManageController;

public class ManageControllerImpl implements ManageController {
    @Override
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }
}
