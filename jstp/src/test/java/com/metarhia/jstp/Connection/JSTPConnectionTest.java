package com.metarhia.jstp.Connection;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSTypes.JSArray;
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
