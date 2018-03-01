package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by lundibundi on 2/22/17.
 */
public class LoopbackAnswer implements Answer<Void> {

  private Connection connection;

  public LoopbackAnswer(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Void answer(InvocationOnMock invocation) throws Throwable {
    JSObject call = JSParser.parse((String) invocation.getArgument(0));
    connection.onMessageParsed(call);
    return null;
  }
}
