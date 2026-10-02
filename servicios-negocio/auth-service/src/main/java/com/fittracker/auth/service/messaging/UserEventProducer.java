package com.fittracker.auth.service.messaging;

import com.fittracker.auth.configuration.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    public void sendUserCreatedEvent(Map<String, Object> payload) {
        log.info("Enviando evento USER_CREATED a exchange: {}, routing key: {}",
                rabbitMQProperties.getExchange(), rabbitMQProperties.getRoutingKey());
        rabbitTemplate.convertAndSend(
                rabbitMQProperties.getExchange(),
                rabbitMQProperties.getRoutingKey(),
                payload
        );
        log.info("Evento USER_CREATED enviado exitosamente");
    }
}
