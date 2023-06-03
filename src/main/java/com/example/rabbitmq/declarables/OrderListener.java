package com.example.rabbitmq.declarables;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderListener {

  @RabbitListener(queues = RabbitConfig.QUEUE_ORDER)
  public void listen(String message){
    log.info("order received :" + message);
  }

}
