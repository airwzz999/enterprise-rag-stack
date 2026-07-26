package com.knowledge.base.file.config;

import com.knowledge.base.common.config.InstanceIdentifier;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ message queue configuration
 * Used for dispatching and consuming asynchronous transcoding tasks
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class RabbitMQConfig {

    public static final String TRANSCODE_EXCHANGE = "transcode.exchange";

    @Resource
    private InstanceIdentifier instanceIdentifier;

    @Bean
    public DirectExchange transcodeExchange() {
        return new DirectExchange(TRANSCODE_EXCHANGE, true, false);
    }

    @Bean
    public Queue transcodeQueue() {
        return new Queue("transcode.queue." + instanceIdentifier.getId(), true, false, false);
    }

    @Bean
    public Binding transcodeBinding() {
        return BindingBuilder.bind(transcodeQueue())
                .to(transcodeExchange())
                .with("transcode." + instanceIdentifier.getId());
    }

    /**
     * Returns the instance-scoped transcode queue name
     * Referenced by the SpEL expression on {@code @RabbitListener}
     */
    public String transcodeQueueName() {
        return "transcode.queue." + instanceIdentifier.getId();
    }
}
