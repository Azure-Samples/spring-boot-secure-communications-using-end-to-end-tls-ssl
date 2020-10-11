/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.secure.greeting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class GreetingController {

    @Value("${application.message:Hello, %s! - but not configured by a Spring Cloud Config Server%n}")
    private String message;

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {
        return String.format(message, name);
    }
}