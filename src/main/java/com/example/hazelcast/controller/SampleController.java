package com.example.hazelcast.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SampleController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, Spring Boot with Hazelcast!";
    }
}