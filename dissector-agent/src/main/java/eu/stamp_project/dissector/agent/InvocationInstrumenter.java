package eu.stamp_project.dissector.agent;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;


public class InvocationInstrumenter implements MethodInstrumenter {

    private String getProbe(String annotation, int id) {
        return String.format("{eu.stamp_project.instrumentation.CallTracer.send(%s:%d:\" + Thread.currentThread().getId() + \":\" + Thread.currentThread().getStackTrace().length);}",
                annotation, id);
    }

    @Override
    public void instrument(CtBehavior behavior, CtClass inClass, int id) throws CannotCompileException {
            behavior.insertBefore(getProbe(">", id));
            behavior.insertAfter(getProbe("<", id));
    }

}
