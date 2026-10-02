package com.fittracker.workout.service.http;

import com.fittracker.workout.service.http.dto.ExerciseSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "routine-service", path = "/api/v1/exercises")
public interface RoutineClient {

    @GetMapping("/{id}")
    ExerciseSummaryDto getExercise(@PathVariable("id") UUID id);

    @GetMapping("/search")
    List<ExerciseSummaryDto> searchExercises(@RequestParam("q") String query);
}
