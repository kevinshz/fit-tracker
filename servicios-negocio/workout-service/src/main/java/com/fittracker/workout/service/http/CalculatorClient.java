package com.fittracker.workout.service.http;

import com.fittracker.workout.service.http.dto.OneRepMaxRequest;
import com.fittracker.workout.service.http.dto.OneRepMaxResponse;
import com.fittracker.workout.service.http.dto.RoundLoadRequest;
import com.fittracker.workout.service.http.dto.RoundLoadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "calculator-service", path = "/api/v1/calculator")
public interface CalculatorClient {

    @PostMapping("/one-rep-max")
    OneRepMaxResponse oneRepMax(@RequestBody OneRepMaxRequest request);

    @PostMapping("/round-load")
    RoundLoadResponse roundLoad(@RequestBody RoundLoadRequest request);
}
