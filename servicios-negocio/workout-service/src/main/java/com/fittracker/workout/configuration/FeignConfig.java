package com.fittracker.workout.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * La configuracion de FeignClients se declara en {@code WorkoutApplication}
 * via {@code @EnableFeignClients}; duplicarla aqui provocaria un
 * BeanDefinitionOverrideException.
 */
@Configuration
public class FeignConfig {
}
