package fr.inria.stamp.dissector.monitor;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;


import java.util.HashSet;
import java.util.Set;

public class TestMethodCollector {

    private ClassPool pool;

    public TestMethodCollector(ClassPool pool) {
        this.pool = pool;
        methods = new HashSet<>();
        failedClasses = new HashSet<>();
    }

    public void collectFrom(String className)  {
        try {
            CtClass container = pool.get(className);
            if (!isTestClass(container)) return;

            for (CtBehavior behavior : container.getDeclaredBehaviors()) {
                MethodInfo info = behavior.getMethodInfo();
                methods.add(container.getName().replace('.', '/') + "/" + info.getName() + info.getDescriptor());
            }
        }
        catch (NotFoundException exc) {
            failedClasses.add(className);
        }
    }

    private boolean isTestClass(CtClass aClass) {
        for(CtMethod method : aClass.getMethods()) {
            MethodInfo info = method.getMethodInfo();
            AnnotationsAttribute attr = (AnnotationsAttribute) info.getAttribute(AnnotationsAttribute.visibleTag);
            if(attr != null && attr.getAnnotation("org.junit.Test") != null)
                return true;
        }
        return false;
    }

    private Set<String> methods;
    public Set<String> getCollectecMethods() {
        return methods;
    }

    private Set<String> failedClasses;
    public Set<String> getFailedClasses() {
        return failedClasses;
    }

}
