package com.fittracker.user.service.messaging;

import com.fittracker.user.persistence.entity.UserProfile;
import com.fittracker.user.persistence.repository.UserProfileRepository;
import com.fittracker.user.utils.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    @RabbitListener(queues = "user.created.queue")
    public void handleUserCreated(Map<String, Object> event) {
        log.info("Evento user.created recibido: {}", event);

        UUID userId = UUID.fromString((String) event.get("userId"));
        String email = (String) event.get("email");
        String name = (String) event.get("name");

        if (repository.existsByUserId(userId)) {
            log.warn("Ya existe perfil para usuario {}, ignorando evento", userId);
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .build();

        repository.save(profile);
        log.info("Perfil creado exitosamente para usuario {}", userId);
    }
}
