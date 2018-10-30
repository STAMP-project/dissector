package eu.stamp_project.dissector.agent;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class StackInstrumenter implements BehaviorInstrumenter {

    @Override
    public void instrument(CtBehavior behavior, int id) throws CannotCompileException {
        behavior.insertBefore(
                "{"
                        + "Exception exc = new Exception();"
                        + "StringBuilder builder = new StringBuilder();"
                        + "builder.append(" + id + ").append(\":\");"
                        + "StackTraceElement[] trace = exc.getStackTrace();"
                        + "for(int i = 0; i < trace.length; i++) {"
                            + "StackTraceElement element = trace[i];"
                            + "builder.append(\"<\")"
                                + ".append(element.getFileName())"
                                + ".append(\":\")"
                                + ".append(element.getClassName())"
                                + ".append(\":\")"
                                + ".append(element.getMethodName())"
                                + ".append(\":\")"
                                + ".append(element.getLineNumber())"
                                + ".append(\">\");"
                        + "}"
                        + "eu.stamp_project.instrumentation.CallTracer.send(builder.toString());"

                + "}"
        );
    }
}
