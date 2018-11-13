package eu.stamp_project.dissector.monitor;

import eu.stamp_project.dissector.monitor.reporting.MethodTracesEntry;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class StackTraceMonitorMojoTest {

    @Test
    public void testTracesForMethods() throws Exception {

        String[] traces = {
                "[[D][1:<BookBean.java:com.hascode.tutorial.service.BookBean:create:-1><OtherTest.java:com.hascode.tutorial.service.OtherTest:shouldFailCreateBookWithNoTitleGiven:29><NativeMethodAccessorImpl.java:sun.reflect.NativeMethodAccessorImpl:invoke0:-2><NativeMethodAccessorImpl.java:sun.reflect.NativeMethodAccessorImpl:invoke:62><DelegatingMethodAccessorImpl.java:sun.reflect.DelegatingMethodAccessorImpl:invoke:43><Method.java:java.lang.reflect.Method:invoke:498><FrameworkMethod.java:org.junit.runners.model.FrameworkMethod$1:runReflectiveCall:50><ReflectiveCallable.java:org.junit.internal.runners.model.ReflectiveCallable:run:12><FrameworkMethod.java:org.junit.runners.model.FrameworkMethod:invokeExplosively:47><InvokeMethod.java:org.junit.internal.runners.statements.InvokeMethod:evaluate:17><ExpectException.java:org.junit.internal.runners.statements.ExpectException:evaluate:19><ParentRunner.java:org.junit.runners.ParentRunner:runLeaf:325><BlockJUnit4ClassRunner.java:org.junit.runners.BlockJUnit4ClassRunner:runChild:78><BlockJUnit4ClassRunner.java:org.junit.runners.BlockJUnit4ClassRunner:runChild:57><ParentRunner.java:org.junit.runners.ParentRunner$3:run:290><ParentRunner.java:org.junit.runners.ParentRunner$1:schedule:71><ParentRunner.java:org.junit.runners.ParentRunner:runChildren:288><ParentRunner.java:org.junit.runners.ParentRunner:access$000:58><ParentRunner.java:org.junit.runners.ParentRunner$2:evaluate:268><ParentRunner.java:org.junit.runners.ParentRunner:run:363><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:execute:369><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:executeWithRerun:275><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:executeTestSet:239><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:invoke:160><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:invokeProviderInSameClassLoader:373><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:runSuitesInProcess:334><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:execute:119><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:main:407>]]",
                "[[D][0:<BookBean.java:com.hascode.tutorial.service.BookBean:getAllBooks:-1><OtherTest.java:com.hascode.tutorial.service.OtherTest:testLibrary:60><NativeMethodAccessorImpl.java:sun.reflect.NativeMethodAccessorImpl:invoke0:-2><NativeMethodAccessorImpl.java:sun.reflect.NativeMethodAccessorImpl:invoke:62><DelegatingMethodAccessorImpl.java:sun.reflect.DelegatingMethodAccessorImpl:invoke:43><Method.java:java.lang.reflect.Method:invoke:498><FrameworkMethod.java:org.junit.runners.model.FrameworkMethod$1:runReflectiveCall:50><ReflectiveCallable.java:org.junit.internal.runners.model.ReflectiveCallable:run:12><FrameworkMethod.java:org.junit.runners.model.FrameworkMethod:invokeExplosively:47><InvokeMethod.java:org.junit.internal.runners.statements.InvokeMethod:evaluate:17><ParentRunner.java:org.junit.runners.ParentRunner:runLeaf:325><BlockJUnit4ClassRunner.java:org.junit.runners.BlockJUnit4ClassRunner:runChild:78><BlockJUnit4ClassRunner.java:org.junit.runners.BlockJUnit4ClassRunner:runChild:57><ParentRunner.java:org.junit.runners.ParentRunner$3:run:290><ParentRunner.java:org.junit.runners.ParentRunner$1:schedule:71><ParentRunner.java:org.junit.runners.ParentRunner:runChildren:288><ParentRunner.java:org.junit.runners.ParentRunner:access$000:58><ParentRunner.java:org.junit.runners.ParentRunner$2:evaluate:268><ParentRunner.java:org.junit.runners.ParentRunner:run:363><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:execute:369><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:executeWithRerun:275><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:executeTestSet:239><JUnit4Provider.java:org.apache.maven.surefire.junit4.JUnit4Provider:invoke:160><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:invokeProviderInSameClassLoader:373><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:runSuitesInProcess:334><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:execute:119><ForkedBooter.java:org.apache.maven.surefire.booter.ForkedBooter:main:407>]]"
        };


        MethodSet methods = new MethodSet(Arrays.asList("getAllBooks", "create"));
        StackTraceMonitorMojo mojo = new StackTraceMonitorMojo();
        mojo.targetsForTheAgent = methods;

        for(String line : traces) {
            mojo.processLine(line);
        }

        MethodTracesEntry[] report = mojo.buildReport();
        assertEquals(methods.size(), report.length);

    }

    @Test
    public void testTraceLength() throws Exception {
        MethodSet methods = new MethodSet(Arrays.asList("dummyMethod"));
        StackTraceMonitorMojo mojo = new StackTraceMonitorMojo();
        mojo.targetsForTheAgent = methods;

        mojo.processLine("[[D][0:<File.java:to.Class1:dummyMethod:1><File.java:to.Class1:another:2>]]");
        mojo.processLine("[[D][0:<File.java:to.Class1:dummyMethod:1><File.java:to.Class1:another:2>]]");

        MethodTracesEntry[] report = mojo.buildReport();
        assertEquals(methods.size(), report.length);
        assertEquals("There should be only one trace", 1, report[0].getTraces().size());
        assertEquals("There should be only 2 entries in the report", 2, report[0].getTraces().get(0).length);

    }




}