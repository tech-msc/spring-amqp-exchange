package com.example.rabbitmq.declarables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {
	private static final String HOST = "localhost";
	private static final int PORT = 5672;
	private static final String USER = "admin";
	private static final String PASS = "admin";
	private static final String VIRTUAL_HOST = "ExampleVirtualHost";

	private static final String EXCHANGE = "com.example.rabbitmq.project.pubsub";
	private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
	private static final long RETRY_TTL = TimeUnit.MINUTES.toMillis(15L);
	private static final long DEAD_LETTER_TTL = TimeUnit.DAYS.toMillis(15L);
	private static final String X_MESSAGE_TTL = "x-message-ttl";

	public static final String QUEUE_ORDER = "com.example.rabbitmq.project.order";
	public static final String QUEUE_STOCK = "com.example.rabbitmq.project.stock";
	public static final String ORDER_ROUTING_KEY = "com.example.rabbitmq.order";

	private Collection<Declarable> orderQueueDeclarations() {
		String queueName = QUEUE_ORDER;
		String routingKey = ORDER_ROUTING_KEY;
		return generateDefaultQueueWithRetryAndDeadletter(queueName, routingKey);
	}

	private Collection<Declarable> stockQueueDeclarations() {
		String queueName = QUEUE_STOCK;
		String routingKey = ORDER_ROUTING_KEY;
		return generateDefaultQueueWithRetryAndDeadletter(queueName, routingKey);
	}

	@Bean
	Declarables topicBindings() {
		Collection<Declarable> exchangesDeclarations = exchangesDeclarations();

		Collection<Declarable> orderQueueDeclarations = orderQueueDeclarations();
		Collection<Declarable> stockQueueDeclarations = stockQueueDeclarations();

		Collection<Declarable> declarablesCollection = new ArrayList();
		declarablesCollection.addAll(exchangesDeclarations);
		declarablesCollection.addAll(orderQueueDeclarations);
		declarablesCollection.addAll(stockQueueDeclarations);

		return new Declarables(declarablesCollection);
	}

	private Collection<Declarable> exchangesDeclarations() {
		TopicExchange exchange = exchangePrincipal();
		TopicExchange exchangeDeadLetter = exchangePrincipalDeadLetter();
		TopicExchange exchangeRetry = exchangePrincipalRetry();

		return List.of(exchange, exchangeDeadLetter, exchangeRetry);
	}

	private Collection<Declarable> generateDefaultQueueWithRetryAndDeadletter(String queueName, String routingKey) {
		Queue queue = generateRegularQueue(queueName);
		Queue queueRetry = generateRegularRetryQueue(queueName);
		Queue queueDeadLetter = generateRegularDeadLetterQueue(queueName);

		Binding queueBind = regularBinding(queue, exchangePrincipal(), routingKey);
		Binding queueBindRetry = regularBinding(queue, exchangePrincipalRetry(), routingKey);
		Binding queueBindDeadLetter = regularBinding(queue, exchangePrincipalDeadLetter(), routingKey);

		return List.of(queue, queueBind, queueRetry, queueBindRetry, queueDeadLetter, queueBindDeadLetter);
	}

	private TopicExchange exchangePrincipal() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	private TopicExchange exchangePrincipalDeadLetter() {
		return new TopicExchange(toDeadLetter(EXCHANGE), true, false);
	}

	private TopicExchange exchangePrincipalRetry() {
		return new TopicExchange(toRetry(EXCHANGE), true, false);
	}

	private Map<String, Object> getDeadLetterArguments() {
		return Collections.singletonMap(X_MESSAGE_TTL, DEAD_LETTER_TTL);
	}

	private Queue generateRegularQueue(String queueDefault) {
		return new Queue(queueDefault, true, false, false, getRegularArguments());
	}

	private Map<String, Object> getRegularArguments() {
		return Collections.singletonMap(X_DEAD_LETTER_EXCHANGE, getDefaultDeadLetterArgumentValue());
	}

	private String getDefaultDeadLetterArgumentValue() {
		return toDeadLetter(EXCHANGE);
	}

	private Map<String, Object> getRetryArguments() {
		final Map<String, Object> retryArguments = new HashMap<>();
		retryArguments.put(X_MESSAGE_TTL, RETRY_TTL);
		retryArguments.put(X_DEAD_LETTER_EXCHANGE, EXCHANGE);
		return retryArguments;
	}

	public static String toDeadLetter(final String resource) {
		return resource.concat("-deadletter");
	}

	private static String toRetry(final String resource) {
		return resource.concat("-retry");
	}

	private Binding regularBinding(Queue queue, TopicExchange exchange, String routingKey) {
		return BindingBuilder.bind(queue).to(exchange).with(routingKey);
	}

	private Queue generateRegularRetryQueue(String queueDefault) {
		return new Queue(toRetry(queueDefault), true, false, false, getRetryArguments());
	}

	private Queue generateRegularDeadLetterQueue(String queueDefault) {
		return new Queue(toDeadLetter(queueDefault), true, false, false, getDeadLetterArguments());
	}

	@Bean
	CachingConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost(HOST);
		connectionFactory.setPort(PORT);
		connectionFactory.setUsername(USER);
		connectionFactory.setPassword(PASS);
		connectionFactory.setVirtualHost(VIRTUAL_HOST);

		return connectionFactory;
	}

	@Bean
	RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

}
