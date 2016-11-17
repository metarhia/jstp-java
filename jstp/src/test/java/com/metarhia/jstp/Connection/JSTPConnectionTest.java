package com.metarhia.jstp.Connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by lundibundi on 9/3/16.
 */
public class JSTPConnectionTest {

    JSTPConnection mConnection;

    @Before
    public void setUp() {
        mConnection = new JSTPConnection("nothing", 4343);
    }

    @After
    public void tearDown() {
        mConnection.close();
        mConnection = null;
    }

//    @Test
//    public void rawTlsConnection() throws Exception {
//        Socket socket = null;
//        InetAddress host = InetAddress.getByName("since.tv");
//        socket = new Socket(host, 4000);

//        installCertificate();
//        SSLContext context = SSLContext.getInstance("TLS");
//        context.init(null, null, new SecureRandom());
//        socket = context.getSocketFactory().createSocket(socket, "since.tv", 4000, true);
//        SSLContext context = SSLContext.getInstance("TLSv1.2");
//        context.init(null, null, null);
//        socket = context.getSocketFactory().createSocket("since.tv", 4000);
//        socket = SSLContext.getDefault().getSocketFactory().createSocket("since.tv", 4000);
//        if (!socket.isConnected()) throw new RuntimeException("no connection");

//        final OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
//        final String s = "{ handshake: [ 0, 'superIn' ] }\0";
//        socket.getOutputStream().write(s.getBytes());
//
//        final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        System.out.println(in.readLine());
//    }

//    @Test
//    public void tlsConnection() throws Exception {
//        final boolean[] valid = {false};
//
//        JSTPConnection connection = new JSTPConnection("since.tv", 4000, true);
//        connection.handshake("superIn", new ManualHandler() {
//            @Override
//            public void invoke(JSValue packet) {
//                valid[0] = true;
//                JSTPConnectionTest.this.notify();
//            }
//        });
//
//        synchronized (this) {
//            wait();
//        }
//
//        assertTrue(valid[0]);
//    }

    @Test
    public void onMessageReceivedCall() throws Exception {
        String packet = "{call:[17,'auth'], newAccount:['Payload data']}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false};
        mConnection.addCallHandler("newAccount", new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        mConnection.onMessageReceived(packet);

        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedEvent() throws Exception {
        String packet = "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false};
        mConnection.addEventHandler("auth", "insert", new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        mConnection.onMessageReceived(packet);

        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedCallback() throws Exception {
        String packet = "{callback:[17],ok:[15703]}" + JSTPConnection.TERMINATOR;


        final Boolean[] success = {false};
        mConnection.addHandler(17, new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        mConnection.onMessageReceived(packet);

        assertTrue(success[0]);
    }

    @Test
    public void onMessageReceivedMultiple() throws Exception {
        String packet = "{error:}" + JSTPConnection.TERMINATOR
            + "{callback:[17],ok:[15703]}" + JSTPConnection.TERMINATOR
            + "{event:[18,'auth'],insert:['Marcus Aurelius','AE127095']}" + JSTPConnection.TERMINATOR;

        final Boolean[] success = {false, false};
        mConnection.addEventHandler("auth", "insert", new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[0] = true;
            }
        });

        mConnection.addHandler(17, new ManualHandler() {
            @Override
            public void invoke(JSValue packet) {
                success[1] = true;
            }
        });

        mConnection.onMessageReceived(packet);

        assertTrue(success[0] && success[1]);
    }

}