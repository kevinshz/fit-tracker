package com.fittracker.calculator.persistence.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Equipment {

    BARBELL(2.5),
    DUMBBELL(2.0),
    MACHINE(5.0);

    private final double incrementKg;

}
