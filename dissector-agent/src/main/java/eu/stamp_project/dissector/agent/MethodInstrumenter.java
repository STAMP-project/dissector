package eu.stamp_project.dissector.agent;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

@FunctionalInterface
public interface MethodInstrumenter {

    void instrument(CtBehavior behavior, CtClass inClass, int id) throws CannotCompileException;

}
