package fr.inria.stamp.dissector;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;



public class MethodTransformer implements ClassFileTransformer {

    public MethodTransformer() { this( c ->true, m -> true); }

    public MethodTransformer(Set<String> targetClasses, Set<String> targetMethods) {
        this( c -> targetClasses.contains(c.getName()), m -> targetMethods.contains(m.getLongName()) );
    }

    public MethodTransformer(Predicate<CtBehavior> targetMethods) { this((x)->true, targetMethods); }

    public MethodTransformer(Predicate<CtClass> targetClasses, Predicate<CtBehavior> targetMethods) {

        if( targetMethods == null) throw new IllegalArgumentException("Method filter can't be null");
        if( targetClasses == null) throw new IllegalArgumentException("Classes filter cant' be null");

        this.targetMethods = targetMethods;
        this.targetClasses = targetClasses;
    }

    private Predicate<CtBehavior> targetMethods;
    private Predicate<CtClass> targetClasses;

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer)
        throws IllegalClassFormatException
    {

        try {
            if(className == null) { //Weird but might happen
                return classFileBuffer;
            }

            ClassPool pool = ClassPool.getDefault();

            CtClass theClass = pool.get(className.replace('/', '.'));

            if(isClassToSkip(theClass)) {
                return classFileBuffer;
            }

//            Arrays.stream(theClass.getDeclaredBehaviors())
//                    .filter(this::isValidMethodTarget)
//                    .forEach(this::instrument);

            for (CtBehavior behavior :
                    theClass.getDeclaredBehaviors()) {
                if (!isValidMethodTarget(behavior)) {
                    methodSkippedEvent.invokeWith(behavior);
                    continue;
                }
                instrument(behavior);
                methodInstrumentedEvent.invokeWith(behavior);
            }

            byte[] outputBuffer = theClass.toBytecode();
            theClass.detach();
            return outputBuffer;
        }
        catch(Throwable exc) {
            StaticDatabase.error(exc.getClass().getName() + ": " + exc.getMessage());
            return classFileBuffer;
        }
    }

    protected String getEnterProbe(int id) {
        return String.format("{fr.inria.stamp.dissector.StaticDatabase.enter(%d, $args);}", id);
    }

    protected String getExitProbe(int id) {
        return String.format("{fr.inria.stamp.dissector.StaticDatabase.exit(%d);}", id);
    }

    protected void instrument(CtBehavior behavior) {
        try {
            int givenID = StaticDatabase.add(behavior);
            behavior.insertBefore(getEnterProbe(givenID));
            behavior.insertAfter(getExitProbe(givenID), true);
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("An error was found while compiling the probes: " + exc.getMessage(), exc);
        }
    }

    private boolean mustSkip(CtBehavior  method) {
        int modifiers = method.getModifiers();
        int check = Modifier.ABSTRACT | Modifier.NATIVE | AccessFlag.SYNTHETIC;
        return (modifiers & check) != 0;
    }

    protected boolean isValidMethodTarget(CtBehavior behavior) { return !mustSkip(behavior) && targetMethods.test(behavior); }

    protected boolean isClassToSkip(CtClass aClass) { return isOurs(aClass) || !targetClasses.test(aClass); }

    private boolean isOurs(CtClass aClass) { return aClass.getPackageName().equals("fr.inria.stamp.dissector"); }


    public final Event<CtBehavior> methodInstrumentedEvent = new Event<>();
    public final Event<CtBehavior> methodSkippedEvent = new Event<>();

//    public final Event<CtClass> classSkippedEvent = new Event<>();
//    public final Event<Throwable> instrumentationErrorEvent = new Event<>();
//    public final Event<CtClass> classInstrumentedEvent = new Event<>();

}
