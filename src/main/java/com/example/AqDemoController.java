package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import oracle.sql.CHAR;
import oracle.sql.NUMBER;

import java.sql.SQLException;

@Controller("/aqDemo")
public class AqDemoController {

  private final AqProducer aqProducer;

  public AqDemoController(AqProducer aqProducer) {
    this.aqProducer = aqProducer;
  }

  @Get(value = "/", produces = MediaType.APPLICATION_JSON)
  public HttpResponse send() throws JsonProcessingException, SQLException {
    AqMessage message = new AqMessage();

    message.ident = new NUMBER(111);
    message.message = new CHAR("abcd", null);

    aqProducer.send(message);
    return HttpResponse.ok();
  }
}