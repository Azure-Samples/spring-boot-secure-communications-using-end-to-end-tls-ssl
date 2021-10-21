/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.secure.greeting.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreetingController {

    @Value("${application.message:Hello, %s! - but not configured by a Spring Cloud Config Server%n}")
    private String message;

    @Value("${application.external.service.endpoint}")
    private String externalServiceEndpoint;

    @Value("${application.external.service.port}")
    private String externalServicePort;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {

        String result = String.format(message, name) + " -- cached hello from WebFlux";
        String externalURI = "https://" + externalServiceEndpoint + ":" +
            externalServicePort + "/hello/" + name;

        try {

            //
            // note change the URL below to correspond to your own service
            // hosted externally from Azure Spring Cloud.
            //
            RestTemplate restTemplate = restTemplateBuilder.build();
            result = restTemplate.getForObject("https://" + externalServiceEndpoint + ":" +
                externalServicePort + "/hello/{name}", String.class, name);

            result = outputPart1 + externalURI + outputPart3 + result + outputPart5;

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            result = sw.toString();

            System.out.println("Error:\n" + result);
        }

        return result;
    }

    @GetMapping("/hello-internal/{name}")
    public String helloInternal(@PathVariable String name) {
        return String.format("Hello, %s! - from the Greeting External Service V2 %n", name);
    }

    @GetMapping("/system")
    public String system(){

        String result = "Cached message from WebFlux";

        try {

            //
            // note change the URL below to correspond to your own service
            // hosted externally from Azure Spring Cloud.
            //
            RestTemplate restTemplate = restTemplateBuilder.build();
            result = restTemplate.getForObject("https://" + externalServiceEndpoint + ":" +
                externalServicePort + "/system", String.class);

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            result = sw.toString();

            System.out.println("Error:\n" + result);
        }

        return result;
    }


    private String outputPart1 = new String(
        "<!DOCTYPE html><html><head><title>Greeting External Service V2</title><style>" +
            "body { background-color: white; text-align: left; color: blacm; font-family: Arial, Helvetica, sans-serif;}" +
            "</style></head><body><h1>Greeting External Service</h1><hr><p>\"greeting-external-service-v2\" &nbsp;&nbsp;&nbsp;&nbsp;: <strong>invoked the \"external-service\" at \"");

    private String outputPart3 = new String(
        "\".</strong></p><p>\"external-service\" responded : <strong>\"");

    private String outputPart5 = new String(
        "\"</strong>.</p> <hr> <h2>Route Taken</h2>" +
            "<style> .demo { border:1px solid #C0C0C0; border-collapse:collapse; padding:5px;}.demo th {" +
            "border:1px solid #C0C0C0; padding:5px; background:#F0F0F0;} " +
            "	.demo td {border:1px solid #C0C0C0;padding:5px;} </style> " +
            "<table class=\"demo\"><thead><tr><th>#</th><th>TLS Segment</th>" +
            "</tr></thead><tbody><tr><td>&nbsp;1</td><td>&nbsp;Consumer to service</td>" +
            "</tr><tr><td>&nbsp;2</td><td>&nbsp;Service ingress controller to Spring Cloud Gateway</td>" +
            "</tr><tr><td>&nbsp;3</td><td>&nbsp;Spring Cloud Gateway to Spring Cloud Service Registry</td>" +
            "</tr><tr><td>&nbsp;4</td><td>&nbsp;Spring Cloud Gateway to \"greeting-external-service-v2\"</td>" +
            "</tr><tr><td>&nbsp;5</td><td>&nbsp;\"greeting-external-service-v2\" to \"external-service\"</td>" +
            "</tr><tbody> </table></body></html>");

}
