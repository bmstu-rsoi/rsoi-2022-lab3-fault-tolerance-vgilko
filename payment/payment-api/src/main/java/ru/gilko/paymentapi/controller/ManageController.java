package ru.gilko.paymentapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static ru.gilko.paymentapi.constants.ControllerUrls.HEALTH_URL;

@RequestMapping(path = "/manage")
public interface ManageController {


    @GetMapping(HEALTH_URL)
    ResponseEntity<?> health();
}
