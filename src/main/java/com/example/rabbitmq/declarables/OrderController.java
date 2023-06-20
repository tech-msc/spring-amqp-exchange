package com.example.rabbitmq.declarables;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class OrderController {

  private final RabbitTemplate template;

  @PostMapping(value = "/order")
  public ResponseEntity<Object> getMethodName(@RequestParam String stringMessage) throws IOException  {
    log.info("send message to queue");    
    
    Message message = new Message(new ObjectMapper().writeValueAsBytes(stringMessage));
    message.getMessageProperties().setContentType(MediaType.APPLICATION_JSON_VALUE);
    
    template.convertAndSend(RabbitConfig.EXCHANGE, 
        RabbitConfig.ORDER_ROUTING_KEY, 
        message);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

}
