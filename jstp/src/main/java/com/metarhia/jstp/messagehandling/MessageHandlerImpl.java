package com.metarhia.jstp.messagehandling;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link MessageHandler} implementation that allows to specify
 * Executor to be used.
 */
public class MessageHandlerImpl implements MessageHandler {

  private static final Logger logger = LoggerFactory.getLogger(MessageHandlerImpl.class);

  private ClearableExecutor executor;

  private MessageHandlerListener listener;

  public MessageHandlerImpl() {
    this(null);
  }

  /**
   * Creates new instance with {@link Executors#newSingleThreadExecutor()} as default executor
   * @param listener events listener
   */
  public MessageHandlerImpl(MessageHandlerListener listener) {
    this(listener, new ClearableExecutorAdapter(Executors.newSingleThreadExecutor()));
  }

  public MessageHandlerImpl(MessageHandlerListener listener, ClearableExecutor executor) {
    this.listener = listener;
    this.executor = executor;
  }

  @Override
  public synchronized void post(String message) {
    executor.execute(new ParserRunnable(message));
  }

  @Override
  public synchronized void clearQueue() {
    executor.clearQueue();
  }

  @Override
  public void setListener(MessageHandlerListener listener) {
    this.listener = listener;
  }

  private class ParserRunnable implements Runnable {

    private String message;

    public ParserRunnable(String message) {
      this.message = message;
    }

    @Override
    public void run() {
      try {
        final Object parseResult = new JSParser(message).parse();
        if (parseResult instanceof JSObject) {
          listener.onMessageParsed((JSObject) parseResult);
        } else {
          listener.onHandlingError(new MessageHandlingException(
              "Unexpected message (expected JSObject): " + message));
        }
      } catch (JSParsingException e) {
        listener.onHandlingError(new MessageHandlingException(
            "Cannot parse message: " + message, e));
      }
    }
  }

  /**
   * Executor that allows removal of all of it's tasks
   */
  public interface ClearableExecutor extends Executor {

    /**
     * Clears current execution queue while also cancelling any currently running tasks
     */
    void clearQueue();
  }

  /**
   * Simple adapter for {@link ExecutorService} to be used as {@link ClearableExecutor}
   */
  public static class ClearableExecutorAdapter implements ClearableExecutor {

    private ExecutorService executorService;

    private List<Future> submittedTasks;

    public ClearableExecutorAdapter(ExecutorService executorService) {
      this.executorService = executorService;
      this.submittedTasks = new ArrayList<>();
    }

    @Override
    public synchronized void execute(Runnable runnable) {
      submittedTasks.add(executorService.submit(runnable));
    }

    @Override
    public synchronized void clearQueue() {
      for (Future task : submittedTasks) {
        task.cancel(true);
      }
      submittedTasks.clear();
    }

    public void shutdown() {
      executorService.shutdown();
    }

    public ExecutorService getExecutorService() {
      return executorService;
    }
  }
}

