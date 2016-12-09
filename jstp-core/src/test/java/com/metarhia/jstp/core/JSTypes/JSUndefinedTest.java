package com.metarhia.jstp.core.JSTypes;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class JSUndefinedTest {

    @Test
    public void equals() throws Exception {
        // must exist only one instance
        assertSame(JSUndefined.get(), JSUndefined.get());
    }
}