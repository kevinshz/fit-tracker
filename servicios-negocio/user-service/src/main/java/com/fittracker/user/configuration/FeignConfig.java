package com.fittracker.user.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * La configuracion de FeignClients se declara en {@code UserApplication}
 * via {@code @EnableFeignClients}; duplicarla aqui provocaria un
 * BeanDefinitionOverrideException.
 */
@Configuration
public class FeignConfig {
}
