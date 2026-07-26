package com.knowledge.base.document.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowledge.base.common.config.InstanceIdentifier;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ message serialization and exchange declaration configuration
 *
 * <p>Overrides the SimpleMessageConverter from Spring Boot's default {@link RabbitAutoConfiguration},
 * using Jackson2Json serialization to ensure messages are sent in JSON format.</p>
 * <p>Configures DefaultClassMapper to ensure the sender adds __TypeId__ to the message headers,
 * so the consumer kb-foundation can correctly deserialize messages to their target type.</p>
 * <p>Also declares the notification exchange, queue, and binding to ensure the exchange/queue
 * already exist when kb-document publishes messages, avoiding messages being silently dropped
 * because kb-foundation has not started yet.</p>
 * <p>The queue name and routing key use InstanceIdentifier for instance isolation, ensuring
 * multiple developers' local environments do not interfere with each other.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    @Resource
    private InstanceIdentifier instanceIdentifier;

    // ======================== Exchange declaration (shared globally) ========================

    /** Notification exchange (all instances share the same TopicExchange) */
    public static final String NOTIFICATION_EXCHANGE = "kb.notification.exchange";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    // ======================== Review notification queue declaration (instance-isolated) ========================

    /** Instance-isolated review notification queue name (exposed for components such as StatisticsEventPublisher) */
    public String reviewNotificationQueueName() {
        return "kb.notification.review.queue." + instanceIdentifier.getId();
    }

    /** Instance-isolated review notification binding routing key pattern (matches submitted / approved / rejected) */
    public String reviewNotificationRoutingKey() {
        return "notification.review." + instanceIdentifier.getId() + ".*";
    }

    @Bean
    public Queue reviewNotificationQueue() {
        return QueueBuilder.durable(reviewNotificationQueueName()).build();
    }

    @Bean
    public Binding reviewNotificationBinding() {
        return BindingBuilder.bind(reviewNotificationQueue())
                .to(notificationExchange())
                .with(reviewNotificationRoutingKey());
    }

    // ======================== Message converter ========================

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages(
                "com.knowledge.base.common.event",
                "com.knowledge.base.document.dto",
                "java.util",
                "java.lang"
        );
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // Enable publisher confirms to detect whether the message successfully reached the exchange
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && correlationData != null) {
                log.error("Message delivery to exchange failed: id={}, cause={}", correlationData.getId(),
                        cause != null ? cause : "exchange does not exist or is unreachable");
            }
        });

        // Enable mandatory mode to detect whether the message was successfully routed to a queue
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Message was not routed to any queue: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText());
        });

        return rabbitTemplate;
    }
}
