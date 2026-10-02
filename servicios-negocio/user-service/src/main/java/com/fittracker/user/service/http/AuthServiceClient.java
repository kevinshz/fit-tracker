package com.fittracker.user.service.http;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/auth/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") UUID id);
}
