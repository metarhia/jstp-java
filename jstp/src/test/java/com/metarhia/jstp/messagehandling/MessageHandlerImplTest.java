package com.metarhia.jstp.messagehandling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSSerializer;
import com.metarhia.jstp.core.JSTypes.JSElements;
import com.metarhia.jstp.exceptions.MessageHandlingException;
import com.metarhia.jstp.messagehandling.MessageHandler.MessageHandlerListener;
import org.junit.jupiter.api.Test;

class MessageHandlerImplTest {

  @Test
  void post() throws InterruptedException {
    MessageHandlerImpl messageHandler = new MessageHandlerImpl();
    MessageHandlerListener listener = mock(MessageHandlerListener.class);
    messageHandler.setListener(listener);

    messageHandler.post(JSSerializer.stringify(JSElements.EMPTY_OBJECT));

    synchronized (MessageHandlerImplTest.this) {
      wait(500);
    }

    verify(listener, times(1))
        .onMessageParsed(JSElements.EMPTY_OBJECT);
  }

  @Test
  void postError() throws InterruptedException {
    MessageHandlerListener listener = mock(MessageHandlerListener.class);
    MessageHandlerImpl messageHandler = new MessageHandlerImpl(listener);

    messageHandler.post("{__invalid_object__}");

    synchronized (MessageHandlerImplTest.this) {
      wait(500);
    }

    verify(listener, never())
        .onMessageParsed(any(JSObject.class));
    verify(listener, times(1))
        .onHandlingError(any(MessageHandlingException.class));
  }

  @Test
  void clearQueue() throws InterruptedException {
    MessageHandlerListener listener = mock(MessageHandlerListener.class);
    MessageHandlerImpl messageHandler = new MessageHandlerImpl(listener);

    messageHandler.post(JSSerializer.stringify(JSElements.EMPTY_OBJECT));
    // this may fail because of the speed of parsing, we may not be able to call
    // clearQueue fast enough
    messageHandler.clearQueue();

    synchronized (MessageHandlerImplTest.this) {
      wait(500);
    }

    verify(listener, never())
        .onMessageParsed(any(JSObject.class));
    verify(listener, never())
        .onHandlingError(any(MessageHandlingException.class));
  }
}
