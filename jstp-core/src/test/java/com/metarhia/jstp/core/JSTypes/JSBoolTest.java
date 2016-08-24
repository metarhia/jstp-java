package com.metarhia.jstp.core.JSTypes;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lundibundi on 8/24/16.
 */
public class JSBoolTest {
    @Test
    public void getValue() throws Exception {
        JSBool bool = new JSBool(true);

        assertTrue(bool.getValue());
    }

    @Test
    public void setValue() throws Exception {
        JSBool bool = new JSBool(true);
        bool.setValue(false);

        assertFalse(bool.getValue());
    }
}