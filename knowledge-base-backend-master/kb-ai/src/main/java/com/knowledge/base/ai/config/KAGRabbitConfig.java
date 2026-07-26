package com.knowledge.base.ai.config;

import com.knowledge.base.common.config.InstanceIdentifier;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KAG RabbitMQ configuration
 *
 * <p>Defines the queues, exchanges, and bindings needed for KAG graph build tasks.
 * Queue names and routing keys are isolated per instance via InstanceIdentifier,
 * ensuring messages don't interfere across multiple developers' local environments.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "kag.enabled", havingValue = "true", matchIfMissing = true)
public class KAGRabbitConfig {

    @Resource
    private InstanceIdentifier instanceIdentifier;

    public static final String EXCHANGE = "kag.graph.exchange";

    public String kagGraphBuildQueueName() {
        return "kag.graph.build.queue." + instanceIdentifier.getId();
    }

    public String kagGraphBuildRoutingKeyAll() {
        return "kag.graph.build." + instanceIdentifier.getId() + ".all";
    }

    public String kagGraphBuildRoutingKeyByIds() {
        return "kag.graph.build." + instanceIdentifier.getId() + ".by_ids";
    }

    public String kagGraphDeleteRoutingKey() {
        return "kag.graph.delete." + instanceIdentifier.getId();
    }

    @Bean
    public TopicExchange kagGraphExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue kagGraphBuildQueue() {
        return QueueBuilder.durable(kagGraphBuildQueueName())
                .withArgument("x-dead-letter-exchange", EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Binding kagGraphBuildAllBinding() {
        return BindingBuilder.bind(kagGraphBuildQueue())
                .to(kagGraphExchange())
                .with(kagGraphBuildRoutingKeyAll());
    }

    @Bean
    public Binding kagGraphBuildByIdsBinding() {
        return BindingBuilder.bind(kagGraphBuildQueue())
                .to(kagGraphExchange())
                .with(kagGraphBuildRoutingKeyByIds());
    }

    @Bean
    public Binding kagGraphDeleteBinding() {
        return BindingBuilder.bind(kagGraphBuildQueue())
                .to(kagGraphExchange())
                .with(kagGraphDeleteRoutingKey());
    }
}
