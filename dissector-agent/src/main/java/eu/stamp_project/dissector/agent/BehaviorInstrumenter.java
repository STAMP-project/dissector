package eu.stamp_project.dissector.agent;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

@FunctionalInterface
public interface BehaviorInstrumenter {

    void instrument(CtBehavior behavior, int id) throws CannotCompileException;

}
