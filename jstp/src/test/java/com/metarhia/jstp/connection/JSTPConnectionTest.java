package com.metarhia.jstp.connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;
import com.metarhia.jstp.transport.TCPTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class JSTPConnectionTest {

    @Spy private JSTPConnection connection;

    private AbstractSocket transport;

    @Before
    public void setUp() {
        transport = mock(AbstractSocket.class);
        connection = spy(new JSTPConnection(transport));
        doAnswer(new HandshakeAnswer(connection)).when(connection).handshake(anyString(), isA(ManualHandler.class));
        doAnswer(new HandshakeAnswer(connection)).when(connection).handshake(anyString(), Mockito.<ManualHandler>isNull());
        when(transport.isConnected()).thenReturn(true);
        connection.handshake(Constants.MOCK_APP_NAME, null);
    }

    @After
    public void tearDown() {
        connection.close();
        connection = null;
    }

    @Test
    public void emptyObject() throws Exception {
        final String s = "{}" + JSTPConnection.TERMINATOR;
        final boolean[] success = {false};
        connection.addSocketListener(new SimpleJSTPConnectionListener() {
            @Override
            public void onPacketRejected(JSObject packet) {
                success[0] = true;
            }
        });

        connection.onPacketReceived((JSObject) JSParser.parse(s));
        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedCall() throws Exception {
        String packet = "{call:[17,'auth'], newAccount:['Payload data']}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false};
        connection.addCallHandler("newAccount", new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        connection.onPacketReceived((JSObject) JSParser.parse(packet));

        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedEvent() throws Exception {
        String packet = "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false};
        connection.addEventHandler("auth", "insert", new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        connection.onPacketReceived((JSObject) JSParser.parse(packet));

        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedCallback() throws Exception {
        String packet = "{callback:[17],ok:[15703]}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false};
        connection.addHandler(17, new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        connection.onPacketReceived((JSObject) JSParser.parse(packet));

        assertTrue(success[0]);
    }

    @Test
    public void checkPingPong() throws Exception {
        String input = "{ping:[42]}" + JSTPConnection.TERMINATOR;
        String response = "{pong:[42]}" + JSTPConnection.TERMINATOR;

        connection.onPacketReceived((JSObject) JSParser.parse(input));

        verify(transport, times(1)).send(response);
    }
}
