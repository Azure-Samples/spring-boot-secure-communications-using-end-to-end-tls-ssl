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
        String response = String.format(message, name);
        return outputPart1 + response + outputPart3;
    }

    private String outputPart1 = new String(
               "<!DOCTYPE html><html><head><title>Greeting Service</title><style>" +
               "body { background-color: white; text-align: left; color: blacm; font-family: Arial, Helvetica, sans-serif;}" +
               "</style></head><body><h1>Greeting Service</h1><hr><p>\"greeting-service\" : \"<strong>");

    private String outputPart3 = new String(
                "</strong>\"</p><hr><h2>Route Taken</h2>" +
                "<style> .demo { border:1px solid #C0C0C0; border-collapse:collapse; padding:5px;}.demo th {" +
                "border:1px solid #C0C0C0; padding:5px; background:#F0F0F0;} " +
                "	.demo td {border:1px solid #C0C0C0;padding:5px;} </style> " +
                "<table class=\"demo\"><thead><tr><th>#</th><th>TLS Segment</th>" +
                "</tr></thead><tbody><tr><td>&nbsp;1</td><td>&nbsp;Consumer to service</td>" +
                "</tr><tr><td>&nbsp;2</td><td>&nbsp;Service ingress controller to Spring Cloud Gateway</td>" +
                "</tr><tr><td>&nbsp;3</td><td>&nbsp;Spring Cloud Gateway to Spring Cloud Service Registry</td>" +
                "</tr><tr><td>&nbsp;4</td><td>&nbsp;Spring Cloud Gateway to \"greeting-service\"</td>" +
                "</tr><tbody> </table></body></html>");

}