package com.metarhia.jstp.core.JSTypes;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lundibundi on 8/24/16.
 */
public class JSNullTest {
    @Test
    public void equals() throws Exception {
        // must exist only one instance
        assertSame(JSNull.get(), JSNull.get());
    }

}