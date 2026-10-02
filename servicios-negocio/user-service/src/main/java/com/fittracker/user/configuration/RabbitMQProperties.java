package com.fittracker.user.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {

    private UserCreated userCreated = new UserCreated();

    @Getter
    @Setter
    public static class UserCreated {
        private String exchange = "user.exchange";
        private String queue = "user.created.queue";
        private String routingKey = "user.created";
    }
}
