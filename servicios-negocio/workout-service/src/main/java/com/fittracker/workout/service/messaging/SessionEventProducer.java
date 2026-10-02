package com.fittracker.workout.service.messaging;

import com.fittracker.workout.configuration.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    public void sendSessionCompletedEvent(UUID sessionId, UUID userId, String muscleLabel, String sessionType) {
        Map<String, Object> payload = Map.of(
                "sessionId", sessionId.toString(),
                "userId", userId.toString(),
                "muscleLabel", muscleLabel,
                "sessionType", sessionType,
                "timestamp", LocalDateTime.now().toString()
        );
        log.info("Enviando evento SESSION_COMPLETED a exchange: {}, routing key: {}",
                rabbitMQProperties.getExchange(), rabbitMQProperties.getRoutingKey());
        rabbitTemplate.convertAndSend(
                rabbitMQProperties.getExchange(),
                rabbitMQProperties.getRoutingKey(),
                payload
        );
        log.info("Evento SESSION_COMPLETED enviado para sesión: {}", sessionId);
    }
}
