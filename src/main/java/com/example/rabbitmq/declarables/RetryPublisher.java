package com.example.rabbitmq.declarables;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.rabbitmq.client.Channel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryPublisher {  
  
  private final RabbitTemplate rabbitTemplate;

  final static String EXCHANGE_RETRY = RabbitConfig.EXCHANGE_RETRY;
  
  public void sendToRetry(Message message, Channel channel) {
    if (numberOfRetriesExceeded(message)) {
      sendToDeadLetterQueue(message, channel);
      return;
    }
    
    rabbitTemplate.send(EXCHANGE_RETRY, getRoutingKey(message), message);
    basicAck(channel, getDeliveryTag(message));
    return;
  }
  
  private static final int MAX_RETRY = 5;
  private boolean numberOfRetriesExceeded(Message message) {
    Integer count = getRetryCount(message);

    if (isNull(count)) {
      return false;
    }
    return count > MAX_RETRY;
  }
  
  private Integer getRetryCount(Message message) {
    List<Map<String, ?>> xdet = Optional.ofNullable(message.getMessageProperties().getXDeathHeader()).orElse(null);
    Integer count = 0;
    if (nonNull(xdet) && !xdet.isEmpty()) {
      count = MapUtils.getIntValue(xdet.get(0), "count");
      log.warn("\n=========\nContador =  {}\n\n=============", count);
    }

    return count;
  }
  
  public void sendToDeadLetterQueue(Message message, Channel channel) {
    try {
      log.info("send to deadletter queue");
      channel.basicNack(getDeliveryTag(message), false, false);
    } catch (Exception e) {
      log.error("error while send to deadletter queue");
    }
  }
  
  private void basicAck(Channel channel, Long deliveryTag) {
    try {
      channel.basicAck(deliveryTag,false);
    } catch (Exception e) {
      log.error("error while ack message");
    }
  }
  
  private Long getDeliveryTag(Message message) {
    return message.getMessageProperties().getDeliveryTag();
  }
  
  private String getRoutingKey(Message message) {
    return Optional.ofNullable(message)
        .map(m -> m.getMessageProperties())
        .map(mp -> mp.getReceivedRoutingKey())
        .orElse(null);    
  }
}
