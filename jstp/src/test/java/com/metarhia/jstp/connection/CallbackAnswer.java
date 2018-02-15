package com.metarhia.jstp.connection;

import com.metarhia.jstp.TestConstants;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSSerializer;
import java.util.ArrayList;
import java.util.List;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by lundibundi on 2/22/17.
 */
public class CallbackAnswer implements Answer<Void> {

  private Connection connection;
  private JSCallback status;
  private String args;

  public CallbackAnswer(Connection connection) {
    this(connection, JSCallback.OK, new ArrayList<>());
  }

  public CallbackAnswer(Connection connection, JSCallback status, List<?> args) {
    this(connection, status, JSSerializer.stringify(args));
  }

  public CallbackAnswer(Connection connection, JSCallback status, String args) {
    this.connection = connection;
    this.status = status;
    this.args = args;
  }

  @Override
  public Void answer(InvocationOnMock invocation) throws Throwable {
    JSObject call = JSParser.parse((String) invocation.getArgument(0));
    long messageNumber = Connection.getMessageNumber(call);
    String response = String.format(TestConstants.TEMPLATE_CALLBACK,
        messageNumber, status.toString(), args);
    final JSObject callbackMessage = JSParser.parse(response);
    connection.onMessageReceived(callbackMessage);
    return null;
  }
}
