package fr.inria.stamp.dissector.agent;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;


public class MethodTransformer implements ClassFileTransformer {

    public MethodTransformer(Map<String, Set<TargetMethod>> targets) {
        if(targets == null) throw new NullPointerException("Given target aggregation is null");
        this.targets = targets;
    }

    private Map<String, Set<TargetMethod>> targets;

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer)
    {

        try {
            if(className == null ||!targets.containsKey(className)) { //Weird but might happen
                return classFileBuffer;
            }

            ClassPool pool = ClassPool.getDefault();
            CtClass theClass = pool.get(className.replace('/', '.'));

            for(TargetMethod targetMethod : targets.get(className)) {
                CtBehavior behavior;
                if(targetMethod.getName().equals("<init>"))
                    behavior = theClass.getConstructor(targetMethod.getDesc());
                else if(targetMethod.getName().equals("<clinit>"))
                    behavior = theClass.getClassInitializer();
                else
                    behavior = theClass.getMethod(targetMethod.getName(), targetMethod.getDesc());
                if (!mustSkip(behavior))
                    instrument(behavior, theClass, targetMethod.line);
            }

            byte[] outputBuffer = theClass.toBytecode();
            theClass.detach();

            return outputBuffer;

        }
        catch(Throwable exc) {
            transformationError.invokeWith(exc);
            return classFileBuffer;
        }
    }

    private String getInstruction(String annotation, int id) {
        return String.format("{System.err.print(\"\\n[[D][%s:%d:\" + Thread.currentThread().getId() + \":\" + Thread.currentThread().getStackTrace().length + \"]]\\n\");}",
                annotation, id);
    }

    private String getEnterProbe(int id) {
        return getInstruction(">", id);
    }

    private String getExitProbe(int id) {
        return getInstruction("<", id);

    }

    private void instrument(CtBehavior behavior, CtClass inClass, int id) {
        try {
            if(behavior.getMethodInfo().isMethod() && !inClass.equals(behavior.getDeclaringClass())) {
                CtMethod method = (CtMethod) behavior;
                behavior = CtNewMethod.delegator(method, inClass);
                inClass.addMethod((CtMethod)behavior);
            }

            behavior.insertBefore(getEnterProbe(id));
            behavior.insertAfter(getExitProbe(id), true);

            behaviorInstrumented.invokeWith(behavior);
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("An error was found while compiling the probes: " + exc.getMessage(), exc);
        }
    }

    private boolean mustSkip(CtBehavior behavior) {
        int modifiers = behavior.getModifiers();
        int check = Modifier.ABSTRACT | Modifier.NATIVE | AccessFlag.SYNTHETIC;
        return (modifiers & check) != 0;
    }

    public final Event<Throwable> transformationError = new Event<>();

    public final Event<CtBehavior> behaviorInstrumented = new Event<>();

}
