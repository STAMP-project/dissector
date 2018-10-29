package eu.stamp_project.dissector.agent;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

public class StackInstrumenter implements MethodInstrumenter {

    @Override
    public void instrument(CtBehavior behavior, CtClass inClass, int id) throws CannotCompileException {
        behavior.insertBefore(
                "{"
                        + "Exception exc = new Exception();"
                        + "StringBuilder trace = new StringBuilder();"
                        + "trace.append(" + id + ").append(\":\");"
                        + "StackTraceElement[] trace = exc.getStackTrace();"
                        + "for(int i = 0; i < trace.length; i++) {"
                            + "StackTraceElement element = trace[i];"
                            + "builder.append(String.format(\"<%s:$s:%s:%s>\", element.getFileName(), element.getClassName(), element.getMethodName(), element.getLineNumber()));"
                        + "}"
                        + "eu.stamp_project.instrumentation.CallTracer.send(builder.toString());"

                + "}"
        );
    }
}
