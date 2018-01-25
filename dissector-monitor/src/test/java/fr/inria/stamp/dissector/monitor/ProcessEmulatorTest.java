package fr.inria.stamp.dissector.monitor;


import com.google.gson.Gson;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;

public class ProcessEmulatorTest {


    @Test
    public void singleThreadValidSequence() {

        //MethodSet methods = mock(MethodSet.class);

        List<String> methods = new ArrayList<>(4);
        methods.add("path.to.Class.method1()");
        methods.add("path.to.Class.method2()");
        methods.add("path.to.Class.method3()");
        methods.add("path.to.ClassTest.test()");

        Set<String> tests = new HashSet<>();
        tests.add("path.to.ClassTest.test()");

        MethodSet methodSet = new MethodSet(methods, tests);
        ProcessEmulator emulator = new ProcessEmulator(methodSet);

        emulator.enter(1,0, 1);
        emulator.enter(1,3, 2);
        emulator.enter(1,2, 3);
        emulator.exit(1,2, 3);
        emulator.enter(1,1, 3);
        emulator.exit(1,1, 3);
        emulator.exit(1,3, 2);
        emulator.exit(1,0, 1);

        assertFalse(emulator.isCorrupt());
        List<MethodEntry> report = emulator.getReport();
        assertThat(report, hasSize(2));

        for(MethodEntry entry : report) {
            List<TestEntry> testReport = entry.getTests();
            assertThat(testReport, hasSize(1));
            TestEntry test = testReport.get(0);
            List<Integer> distances = test.getDistances();


            assertTrue(methodSet.isTest(test.getTest()));
            assertThat(distances, hasSize(1)); //Issues with genericity and the combination of hamcrest's matchers
            assertThat(distances, hasItem(1));
        }

    }

    @Test
    public void methodCalledTwiceAtSameDepth() {
        List<String> methods = new ArrayList<>(4);
        methods.add("path.to.Class.method1()");
        methods.add("path.to.ClassTest.test()");

        Set<String> tests = new HashSet<>();
        tests.add("path.to.ClassTest.test()");

        MethodSet methodSet = new MethodSet(methods, tests);
        ProcessEmulator emulator = new ProcessEmulator(methodSet);

        emulator.enter(1,1, 1);
        emulator.enter(1, 0, 2);
        emulator.exit(1, 0, 2);
        emulator.enter(1, 0, 2);
        emulator.exit(1, 0, 2);
        emulator.exit(1,1, 1);

        assertFalse(emulator.isCorrupt());

        List<MethodEntry> report = emulator.getReport();
        assertThat(report, hasSize(1));

        MethodEntry entry = report.get(0);
        List<TestEntry> testReport = entry.getTests();

        assertThat(testReport, hasSize(1));

        TestEntry testEntry = testReport.get(0);
        List<Integer> distances = testEntry.getDistances();

        assertThat(distances, hasSize(1));
        assertThat(distances, hasItem(1));

    }

}