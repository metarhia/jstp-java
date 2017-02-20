package com.metarhia.jstp.Connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class JSTPConnectionTest {

    private JSTPConnection connection;

    @Before
    public void setUp() {
        connection = new JSTPConnection("nothing", 4343);
    }

    @After
    public void tearDown() {
        connection.close();
        connection = null;
    }

    @Test
    public void emptyObject() throws Exception {
        final String s = "{}" + JSTPConnection.TERMINATOR;
        connection.onMessageReceived((JSObject) JSParser.parse(s));
        assertTrue(true);
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

        connection.onMessageReceived((JSObject) JSParser.parse(packet));

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

        connection.onMessageReceived((JSObject) JSParser.parse(packet));

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

        connection.onMessageReceived((JSObject) JSParser.parse(packet));

        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedMultiple() throws Exception {
        String packet = "{error:}" + JSTPConnection.TERMINATOR
                + "{callback:[17],ok:[15703]}" + JSTPConnection.TERMINATOR
                + "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false, false};
        connection.addEventHandler("auth", "insert", new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        connection.addHandler(17, new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[1] = true;
            }
        });

        connection.onMessageReceived(packet);

        assertTrue(success[0] && success[1]);
    }

    @Test
    public void checkPingPong() throws Exception {
        String input = "{ping:[42]}" + JSTPConnection.TERMINATOR;
        String response = "{pong:[42]}" + JSTPConnection.TERMINATOR;
        AbstractSocket socket = mock(AbstractSocket.class);

        JSTPConnection connection = new JSTPConnection("", 0);
        connection.createNewConnection(socket);

        connection.onMessageReceived((JSObject) JSParser.parse(input));

        verify(socket, times(1)).sendMessage(response);
    }
}
