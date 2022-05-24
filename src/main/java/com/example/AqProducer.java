package com.example;

import io.micronaut.jms.annotations.JMSProducer;
import io.micronaut.jms.annotations.Queue;
import io.micronaut.messaging.annotation.MessageBody;

@JMSProducer("aqConnectionFactory")
public interface AqProducer {
  @Queue(value = "AQDEMOADMIN.EVENT_QUEUE", serializer = "AqDefaultSerializerDeserializer")
  void send(@MessageBody AqMessage body);
}
