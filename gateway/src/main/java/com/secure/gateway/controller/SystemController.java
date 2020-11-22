package com.secure.gateway.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
public class SystemController {
    private final ReactiveDiscoveryClient reactiveDiscoveryClient;

    @GetMapping(value = "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DetailsResponse> instanceDetails() {
        return reactiveDiscoveryClient.getServices()
                .parallel(5)
                .map(service -> new Service(service, null))
                .doOnNext(service -> getServiceInstances(service)
                        .parallel(5)
                        .doOnNext(serviceInstance -> {
                            log.info("------------ intance of {} -----------", service.getName());
                            log.info("Id: {}", serviceInstance.getInstanceId());
                            log.info("Host: {}", serviceInstance.getHost());
                            log.info("Port: {}", serviceInstance.getPort());
                            log.info("Scheme: {}", serviceInstance.getScheme());
                            log.info("Port: {}", serviceInstance.isSecure());
                        })
                        .map(service::addInstance))
                .sequential()
                .collectList()
                .map(DetailsResponse::new);
    }

    private Flux<ServiceInstance> getServiceInstances(Service service) {
        log.info("Getting instance details of service: {}", service.getName());
        return reactiveDiscoveryClient.getInstances(service.getName());
    }

    @Data
    @AllArgsConstructor
    static class DetailsResponse {
        private List<Service> services;

        DetailsResponse addService(Service service) {
            if (services == null) {
                services = new ArrayList<>();
            }
            services.add(service);
            return this;
        }
    }

    @Data
    @AllArgsConstructor
    static class Service {
        private String name;
        private List<Instance> instances;

        Service addInstance(ServiceInstance instance) {
            if (instances == null) {
                instances = new ArrayList<>();
            }
            instances.add(new Instance(instance));
            return this;
        }
    }

    @Data
    static class Instance {
        private String instanceId;
        private String serviceId;
        private String scheme;
        private String host;
        private int port;
        private boolean secured;
        public Instance(ServiceInstance serviceInstance) {
            this.instanceId = serviceInstance.getInstanceId();
            this.serviceId = serviceInstance.getServiceId();
            this.scheme = serviceInstance.getScheme();
            this.host = serviceInstance.getHost();
            this.port = serviceInstance.getPort();
            this.secured = serviceInstance.isSecure();
        }
    }
}
