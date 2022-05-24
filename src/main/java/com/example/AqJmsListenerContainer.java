package com.example;

import io.micronaut.jms.listener.ConcurrentMessageHandler;
import io.micronaut.jms.listener.JMSListenerContainer;
import io.micronaut.jms.listener.MessageHandler;
import io.micronaut.jms.listener.MessageHandlerAdapter;
import io.micronaut.jms.model.JMSDestinationType;
import io.micronaut.jms.pool.JMSConnectionPool;
import io.micronaut.messaging.exceptions.MessageListenerException;
import oracle.jms.AQjmsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;

public class AqJmsListenerContainer<T> extends JMSListenerContainer<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(JMSListenerContainer.class);
  private static final long DEFAULT_KEEP_ALIVE_TIME = 5; // TODO configurable
  private static final int DEFAULT_EXECUTOR_QUEUE_SIZE = 10; // TODO configurable
  private static final boolean DEFAULT_TRANSACTED = false; // TODO configurable
  private static final int DEFAULT_ACKNOWLEDGE_MODE = AUTO_ACKNOWLEDGE; // TODO configurable

  private final Set<Connection> openConnections = new HashSet<>();
  private final JMSConnectionPool connectionPool;
  private final int threadPoolSize;
  private final int maxThreadPoolSize;
  private final JMSDestinationType type;

  public AqJmsListenerContainer(JMSConnectionPool connectionPool, JMSDestinationType type, int threadPoolSize) {
    this(connectionPool, type, threadPoolSize, threadPoolSize);
  }

  public AqJmsListenerContainer(JMSConnectionPool connectionPool, JMSDestinationType type, int threadPoolSize, int maxThreadPoolSize) {
    super(connectionPool, type, threadPoolSize, maxThreadPoolSize);
    this.connectionPool = connectionPool;
    this.type = type;
    this.threadPoolSize = threadPoolSize;
    this.maxThreadPoolSize = maxThreadPoolSize;
  }

  @Override
  public void registerListener(String destination, MessageHandler<T> listener, Class<T> clazz) {
    try {
      final Connection connection = connectionPool.createConnection();
      final Session session = connection.createSession(DEFAULT_TRANSACTED, DEFAULT_ACKNOWLEDGE_MODE);
      openConnections.add(connection);
      var owner = destination.split("\\.")[0];
      var queue = destination.split("\\.")[1];
      final MessageConsumer consumer = ((AQjmsSession) session).createConsumer(
              ((AQjmsSession) session).getQueue(owner, queue),
              "",
              AqMessage.getORADataFactory(), null,
              true);
      consumer.setMessageListener(
              new MessageHandlerAdapter<>(
                      new ConcurrentMessageHandler<>(
                              listener,
                              new ThreadPoolExecutor(
                                      threadPoolSize,
                                      maxThreadPoolSize,
                                      DEFAULT_KEEP_ALIVE_TIME,
                                      SECONDS,
                                      new LinkedBlockingQueue<>(DEFAULT_EXECUTOR_QUEUE_SIZE),
                                      Executors.defaultThreadFactory())),
                      clazz));
      LOGGER.debug("registered {} listener {} for destination '{}' and class {}",
              type.name().toLowerCase(), listener, destination, clazz.getName());
    } catch (Exception e) {
      throw new MessageListenerException(
              "Problem registering a MessageConsumer for " + destination, e);
    }
  }

  @Override
  public void registerListener(String destination, MessageListener listener, Class<T> clazz, boolean transacted, int acknowledgeMode) {
    try {
      final Connection connection = connectionPool.createConnection();
      final Session session = connection.createSession(transacted, acknowledgeMode);
      openConnections.add(connection);
      var owner = destination.split("\\.")[0];
      var queue = destination.split("\\.")[1];
      final MessageConsumer consumer = ((AQjmsSession) session).createConsumer(
              ((AQjmsSession) session).getQueue(owner, queue),
              "",
              AqMessage.getORADataFactory(), null,
              true);
      consumer.setMessageListener((message) -> {
        try {
          listener.onMessage(message);
          if (transacted) {
            session.commit();
          }
        } catch (Exception e) {
          if (transacted) {
            try {
              session.rollback();
            } catch (JMSException | RuntimeException e2) {
              throw new MessageListenerException(
                      "Problem rolling back transaction", e2);
            }
          }
          throw new MessageListenerException(e.getMessage(), e);
        }
      });
      LOGGER.debug("registered {} listener {} for destination '{}'; " +
                      "transacted: {}, ack mode: {}",
              type.name().toLowerCase(), listener, destination, transacted, acknowledgeMode);
    } catch (JMSException | RuntimeException e) {
      throw new MessageListenerException(
              "Problem registering a MessageConsumer for " + destination, e);
    }
  }
}
