package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSTypes.JSObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Created by lundibundi on 2/22/17.
 */
public class HandshakeAnswer implements Answer<Void> {

    private JSTPConnection connection;

    public HandshakeAnswer(JSTPConnection connection) {
        this.connection = connection;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
        final JSObject handshakePacket = new JSParser(Constants.MOCK_HANDSHAKE_RESPONSE).parseObject();
        connection.onPacketReceived(handshakePacket);
        final ManualHandler handler = invocation.getArgument(1);
        if (handler != null) handler.invoke(handshakePacket);
        return null;
    }
}
