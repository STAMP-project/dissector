package fr.inria.stamp.dissector.agent;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


public class MethodTransformer implements ClassFileTransformer {

    public MethodTransformer(List<String> methods, Set<String> classes) {

        if(classes == null) throw new NullPointerException("Set of classes is null");
        if(methods == null) throw new NullPointerException("Stream of methods is null");

        //We want to keep the position in the stream
        //And searching fast, but don't want to sort them in any way
        buildTreeMap(methods);
        this.classes = classes;

    }


    private void buildTreeMap(List<String> methods) {

        methodPosition = new TreeMap<>();
        AtomicInteger position = new AtomicInteger();

        methods.forEach( m -> {
            methodPosition.put(m, position.getAndIncrement());
        } );

    }

    private Set<String> classes;
    private TreeMap<String, Integer> methodPosition;

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer)
    {

        try {
            if(className == null) { //Weird but might happen
                return classFileBuffer;
            }

            ClassPool pool = ClassPool.getDefault();
            CtClass theClass = pool.get(className.replace('/', '.'));

            if(mustSkip(theClass)) {
                return classFileBuffer;
            }

            for(CtBehavior behavior: theClass.getDeclaredBehaviors()) {
                if(mustSkip(behavior)) continue;
                instrument(behavior);
                behaviorInstrumented.invokeWith(behavior);
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

    private void instrument(CtBehavior behavior) {
        try {
            int givenID = methodPosition.get(behavior.getLongName());
            behavior.insertBefore(getEnterProbe(givenID));
            behavior.insertAfter(getExitProbe(givenID), true);
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("An error was found while compiling the probes: " + exc.getMessage(), exc);
        }
    }

    private boolean mustSkip(CtBehavior behavior) {
        return isNotATarget(behavior) || !methodPosition.containsKey(behavior.getLongName());
    }

    private boolean isNotATarget(CtBehavior  method) {
        int modifiers = method.getModifiers();
        int check = Modifier.ABSTRACT | Modifier.NATIVE | AccessFlag.SYNTHETIC;
        return (modifiers & check) != 0;
    }

    private boolean mustSkip(CtClass aClass) { return isOurs(aClass) || !classes.contains(aClass.getName()); }

    private boolean isOurs(CtClass aClass) { return aClass.getPackageName().equals("fr.inria.stamp.dissector"); }

    public final Event<Throwable> transformationError = new Event<>();

    public final Event<CtBehavior> behaviorInstrumented = new Event<>();

}
