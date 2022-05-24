package com.example;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.jms.listener.JMSListenerContainer;
import io.micronaut.jms.listener.MessageHandler;
import io.micronaut.jms.model.JMSDestinationType;
import io.micronaut.jms.pool.JMSConnectionPool;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.jms.MessageListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Replaces(io.micronaut.jms.listener.JMSListenerContainerFactory.class)
public class AqJmsListenerContainerFactory extends io.micronaut.jms.listener.JMSListenerContainerFactory {
  private static final int THREAD_POOL_SIZE = 1; // TODO configurable?

  private final Logger logger = LoggerFactory.getLogger(getClass());

  // TODO one listener per destination??? at least error/warn if replacing
  private final Map<String, JMSListenerContainer<?>> listeners = new ConcurrentHashMap<>();

  /**
   * Generates a new {@link JMSListenerContainer} and registers the
   * provided {@link MessageHandler} as the receiving executable method.
   * <p>
   * NOTE: this method is not recommended for registering new {@link JMSListenerContainer}s
   * however can be used to register them dynamically and allow for safe shutdown
   * within the Bean Context.
   *
   * @param connectionPool the pool
   * @param destination    the queue or topic name
   * @param listener       the message listener
   * @param clazz          the message type
   * @param type           the destination type
   * @param <T>            the class type
   */
  public <T> void registerListener(final JMSConnectionPool connectionPool,
                                   final String destination,
                                   final MessageHandler<T> listener,
                                   final Class<T> clazz,
                                   final JMSDestinationType type) {
    final JMSListenerContainer<T> container = new JMSListenerContainer<>(
            connectionPool, type, THREAD_POOL_SIZE);
    container.registerListener(destination, listener, clazz);
    listeners.put(destination, container);
    logger.debug("registered {} listener for '{}' {} for type '{}' and pool {}",
            type.name().toLowerCase(), destination, listener, clazz.getName(), connectionPool);
  }

  /**
   * Generates a new {@link JMSListenerContainer} and registers the
   * provided {@link MessageListener} as the receiving executable method.
   * <p>
   * NOTE: this method is not recommended for registering new
   * {@link JMSListenerContainer}s however it can be used to register them
   * dynamically and allow for safe shutdown within the Bean Context.
   *
   * @param connectionPool  the pool
   * @param destination     the queue or topic name
   * @param listener        the message listener
   * @param clazz           the message type
   * @param transacted      indicates whether the session will use a local transaction
   * @param acknowledgeMode when transacted is false, indicates how messages
   *                        received by the session will be acknowledged
   * @param type            the destination type
   * @param <T>             the class type
   * @see javax.jms.Session#AUTO_ACKNOWLEDGE
   * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
   * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
   */
  public <T> void registerListener(final JMSConnectionPool connectionPool,
                                   final String destination,
                                   final MessageListener listener,
                                   final Class<T> clazz,
                                   final boolean transacted,
                                   final int acknowledgeMode,
                                   final JMSDestinationType type) {
    final JMSListenerContainer<T> container = new AqJmsListenerContainer<>(
            connectionPool, type, THREAD_POOL_SIZE);
    container.registerListener(destination, listener, clazz, transacted, acknowledgeMode);
    listeners.put(destination, container);
    logger.debug("registered {} listener for '{}' {} for type '{}'" +
                    " and pool {}; transacted: {}, ack mode {}",
            type.name().toLowerCase(), destination, listener, clazz.getName(),
            connectionPool, transacted, acknowledgeMode);
  }

  /**
   * The {@link JMSListenerContainer} that is listening to the given {@code destination}.
   *
   * @param destination the name of a queue or topic
   * @return the container
   */
  public JMSListenerContainer<?> getRegisteredListener(String destination) {
    return listeners.get(destination);
  }

  /**
   * Shuts down the listeners registered with this factory.
   */
  @PreDestroy
  public void shutdown() {
    for (JMSListenerContainer<?> listener : listeners.values()) {
      listener.shutdown();
    }
  }

  @Override
  public String toString() {
    return "JMSListenerContainerFactory{listeners=" + listeners + '}';
  }

}
