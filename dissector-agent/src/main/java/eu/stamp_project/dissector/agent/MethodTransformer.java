package eu.stamp_project.dissector.agent;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;


public class MethodTransformer implements ClassFileTransformer {

    public MethodTransformer(Map<String, Set<TargetMethod>> targets, MethodInstrumenter instrumenter) {
        if(targets == null) throw new NullPointerException("Given target aggregation is null");
        if(instrumenter == null) throw new NullPointerException("Instrumenter can not be null");
        this.targets = targets;
    }

    private Map<String, Set<TargetMethod>> targets;
    private MethodInstrumenter instrumenter;

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
                CtBehavior behavior = getBehaviorFromClass(targetMethod, theClass);
                if (mustSkip(behavior)) continue;
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

    private CtBehavior getBehaviorFromClass(TargetMethod method, CtClass theClass) throws NotFoundException {

        switch(method.getName()) {
            case "<init>":
                return theClass.getConstructor(method.getDesc());
            case "<clinit>":
                return theClass.getClassInitializer();
            default:
                return theClass.getMethod(method.getName(), method.getDesc());
        }

    }

    private boolean mustSkip(CtBehavior behavior) {
        int modifiers = behavior.getModifiers();
        int check = Modifier.ABSTRACT | Modifier.NATIVE | AccessFlag.SYNTHETIC;
        return (modifiers & check) != 0;
    }

    private void instrument(CtBehavior behavior, CtClass inClass, int id) {
        try {
            if (behavior.getMethodInfo().isMethod() && !inClass.equals(behavior.getDeclaringClass())) {
                CtMethod method = (CtMethod) behavior;
                behavior = CtNewMethod.delegator(method, inClass);
                inClass.addMethod((CtMethod) behavior);
            }
            instrumenter.instrument(behavior, inClass, id);
            behaviorInstrumented.invokeWith(behavior);
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("An error was found while compiling the probes: " + exc.getMessage(), exc);
        }
    }

    public final Event<Throwable> transformationError = new Event<>();

    public final Event<CtBehavior> behaviorInstrumented = new Event<>();

}
