package ru.gilko.carsimpl.controller_impl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.gilko.carsapi.controller.ManageController;

@RestController
public class ManageControllerImpl implements ManageController {
    @Override
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().build();
    }
}
