package fr.inria.stamp.dissector.monitor;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;

import java.util.HashSet;
import java.util.Set;

public class TestMethodCollector {

    private ClassPool pool;
    private HashSet<TestInfo> tests;
    private CtClass testCaseClass;

    public TestMethodCollector(ClassPool pool) throws NotFoundException {
        this.pool = pool;
        testCaseClass = pool.get("junit.framework.TestCase");
        tests = new HashSet<>();
    }

    public void collectFrom(String className)  throws NotFoundException {

        CtClass container = pool.get(className);
        if (!isTestClass(container)) return;

        for (CtBehavior behavior : container.getDeclaredBehaviors()) {

            MethodInfo info = behavior.getMethodInfo();

            if(!Modifier.isPublic(behavior.getModifiers()) || (info.isConstructor() && ((CtConstructor)behavior).isEmpty()))
                continue; // Not counting private methods or default constructors
            //TODO: This constructor filter should be applied to the other collector to reduce the number of methods targeted by the agent

            tests.add(new TestInfo(container.getName().replace('.', '/') + "/" + info.getName() + info.getDescriptor(),
                    isTestCase(info)));

        }
    }

    private boolean isTestCase(MethodInfo info) {
        AnnotationsAttribute attr = (AnnotationsAttribute) info.getAttribute(AnnotationsAttribute.visibleTag);
        return (attr != null && attr.getAnnotation("org.junit.Test") != null);
    }

    private boolean isTestClass(CtClass aClass) {
        if(aClass.subclassOf(testCaseClass))
            return true;
        for(CtMethod method : aClass.getMethods()) {
            if(isTestCase(method.getMethodInfo()))
                return true;
        }
        return false;
    }

    public Set<TestInfo> getTestMethods() {
        return tests;
    }

}
