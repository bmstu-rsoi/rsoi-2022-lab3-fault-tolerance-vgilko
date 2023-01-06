package ru.gilko.rentalapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static ru.gilko.rentalapi.constants.ControllerUrls.HEALTH_URL;

@RequestMapping(path = "/manage")
public interface ManageController {


    @GetMapping(HEALTH_URL)
    ResponseEntity<?> health();
}
