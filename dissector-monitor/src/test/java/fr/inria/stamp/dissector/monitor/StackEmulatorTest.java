package fr.inria.stamp.dissector.monitor;

import com.google.gson.Gson;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StackEmulatorTest {

    @Test
    public void validSequence() {

        MethodSet methods = mock(MethodSet.class);

        when(methods.isTest(3)).thenReturn(true);

        StackEmulator emulator = new StackEmulator(methods);

        emulator.enter(0, 1);
        emulator.enter(3, 2);
        emulator.enter(2, 3);
        emulator.exit(2, 3);
        emulator.enter(1, 3);
        emulator.exit(1, 3);
        emulator.exit(3, 2);
        emulator.exit(0, 1);

        assertFalse(emulator.isCorrupt());

    }

}