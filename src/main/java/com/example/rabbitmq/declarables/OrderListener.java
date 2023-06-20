package com.example.rabbitmq.declarables;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

  private final RabbitTemplate rabbitTemplate;
  //private final RetryPublisher retryPublisher;

  @RabbitListener(queues = RabbitConfig.QUEUE_ORDER, containerFactory = "rabbitContainerFactory")
  public void listen(Message message, Channel channel) throws IOException {
    log.info("order received :" + message.getMessageProperties());

    String routingKey = message.getMessageProperties().getReceivedRoutingKey();
    rabbitTemplate.send(RabbitConfig.EXCHANGE, routingKey, message);
    
    // test retry flow
    // retryPublisher.sendToRetry(message, channel);

    return;
  }

}
