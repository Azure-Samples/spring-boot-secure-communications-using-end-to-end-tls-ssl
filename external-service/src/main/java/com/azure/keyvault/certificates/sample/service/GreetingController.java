/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.azure.keyvault.certificates.sample.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class GreetingController {


    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {
        return String.format("Hello, %s! - from the External Service %n", name);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World - from the External Service !";
    }

}
