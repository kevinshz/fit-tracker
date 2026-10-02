package com.fittracker.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "fittracker.exchange";

    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String USER_CREATED_ROUTING_KEY = "user.created";

    public static final String WORKOUT_SESSION_COMPLETED_QUEUE = "workout.session.completed.queue";
    public static final String WORKOUT_SESSION_COMPLETED_ROUTING_KEY = "workout.session.completed";

    @Bean
    public TopicExchange fittrackerExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(USER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue workoutSessionCompletedQueue() {
        return new Queue(WORKOUT_SESSION_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, TopicExchange fittrackerExchange) {
        return BindingBuilder.bind(userCreatedQueue)
                .to(fittrackerExchange)
                .with(USER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding workoutSessionCompletedBinding(Queue workoutSessionCompletedQueue, TopicExchange fittrackerExchange) {
        return BindingBuilder.bind(workoutSessionCompletedQueue)
                .to(fittrackerExchange)
                .with(WORKOUT_SESSION_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
