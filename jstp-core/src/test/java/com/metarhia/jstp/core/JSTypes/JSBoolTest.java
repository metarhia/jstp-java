package com.metarhia.jstp.core.JSTypes;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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