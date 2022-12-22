package ru.gilko.carsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static ru.gilko.carsapi.constants.ControllerUrl.HEALTH_URL;

@RequestMapping(path = "/manage")
public interface ManageController {


    @GetMapping(HEALTH_URL)
    ResponseEntity<?> health();
}
