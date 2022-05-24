package com.example;

import io.micronaut.jms.annotations.JMSListener;
import io.micronaut.jms.annotations.Queue;
import io.micronaut.messaging.annotation.MessageBody;
import lombok.extern.slf4j.Slf4j;
import java.sql.SQLException;

import static javax.jms.Session.CLIENT_ACKNOWLEDGE;

@JMSListener("aqConnectionFactory")
@Slf4j
public class AqConsumer {

  @Queue(value = "aqdemoadmin.event_queue", concurrency = "1-1", acknowledgeMode = CLIENT_ACKNOWLEDGE)
  public void receive(@MessageBody AqMessage body) throws SQLException {
    log.info("{} {}", body.ident.intValue(), body.message);
  }
}