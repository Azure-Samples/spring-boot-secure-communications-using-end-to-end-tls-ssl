/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.secure.greeting.service;

import java.security.KeyStore;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.io.PrintWriter;

@RestController
public class GreetingController {

    @Value("${application.message:Hello, %s! - but not configured by a Spring Cloud Config Server%n}")
    private String message;

    @Value("${application.external.service.endpoint}")
    private String externalServiceEndpoint;

    @Value("${application.external.service.port}")
    private String externalServicePort;

    @Bean
    public RestTemplate restTemplate() throws Exception {
        KeyStore ks = KeyStore.getInstance("AzureKeyVault");
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(ks, new TrustSelfSignedStrategy())
                .build();

        HostnameVerifier allowAll = (String hostName, SSLSession session) -> true;
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, allowAll);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory
                = new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {

        String result = String.format(message, name) + " -- cached hello from WebFlux";

        try {

            //
            // note change the URL below to correspond to your own service
            // hosted externally from Azure Spring Cloud.
            //
            result = restTemplate().getForObject("https://" + externalServiceEndpoint + ":" +
                externalServicePort + "/hello/{name}", String.class, name);

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
        return String.format("Hello, %s! - from the Greeting External Service %n", name);
    }

}
