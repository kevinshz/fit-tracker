package com.fittracker.messaging;

import com.fittracker.messaging.dto.UserCreatedEvent;
import com.fittracker.messaging.dto.WorkoutSessionCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Messaging DTOs - Tests Unitarios")
class MessagingDtoTest {

    @Test
    @DisplayName("UserCreatedEvent es un record con todos los campos")
    void userCreatedEvent_fields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        var event = new UserCreatedEvent(id, "test@example.com", "Test", now);

        assertThat(event.userId()).isEqualTo(id);
        assertThat(event.email()).isEqualTo("test@example.com");
        assertThat(event.name()).isEqualTo("Test");
        assertThat(event.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UserCreatedEvent tiene equals basado en valores")
    void userCreatedEvent_equals() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);
        var a = new UserCreatedEvent(id, "a@b.com", "A", now);
        var b = new UserCreatedEvent(id, "a@b.com", "A", now);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("WorkoutSessionCompletedEvent es un record con todos los campos")
    void workoutSessionCompletedEvent_fields() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        var event = new WorkoutSessionCompletedEvent(sessionId, userId, "CHEST_BACK", now);

        assertThat(event.sessionId()).isEqualTo(sessionId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.muscleLabel()).isEqualTo("CHEST_BACK");
        assertThat(event.completedAt()).isEqualTo(now);
    }
}
