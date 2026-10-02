package com.fittracker.routine.service.interfaces;

import com.fittracker.routine.presentation.dto.ExerciseRequest;
import com.fittracker.routine.presentation.dto.ExerciseResponse;
import com.fittracker.routine.presentation.dto.ExerciseSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ExerciseService {

    Page<ExerciseSummaryDto> findAll(String muscle, String equipment, Pageable pageable);

    Page<ExerciseSummaryDto> search(String query, Pageable pageable);

    ExerciseResponse findById(UUID id);

    ExerciseResponse create(ExerciseRequest request, String createdBy);

    ExerciseResponse update(UUID id, ExerciseRequest request);

    void delete(UUID id);
}
