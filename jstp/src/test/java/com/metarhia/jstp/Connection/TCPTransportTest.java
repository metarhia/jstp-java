package com.metarhia.jstp.Connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSValue;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

/**
 * Created by lundibundi on 2/18/17.
 */
public class TCPTransportTest {

    private TCPTransport tcpTransport;
    private JSTPConnection connection;

    @Before
    public void setUp() {
        connection = new JSTPConnection("", 0);

        tcpTransport = new TCPTransport("", 0);
        connection.createNewConnection(tcpTransport);
    }

    @Test
    public void onMessageReceivedMultiple() throws Exception {
        String packet = "{error:12}" + JSTPConnection.TERMINATOR
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

        Field inputField = TCPTransport.class.getDeclaredField("in");
        inputField.setAccessible(true);
        BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(packet.getBytes()));
        inputField.set(tcpTransport, in);

        final Method method = TCPTransport.class.getDeclaredMethod("processMessage");
        method.setAccessible(true);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (tcpTransport != null) method.invoke(tcpTransport);
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
            }
        });
        thread.start();

        synchronized (TCPTransportTest.this) {
            wait(3000);
            thread.interrupt();
        }

        assertTrue(success[0] && success[1]);
    }

}