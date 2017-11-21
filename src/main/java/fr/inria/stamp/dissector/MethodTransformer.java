package fr.inria.stamp.dissector;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.function.Predicate;


public class MethodTransformer implements ClassFileTransformer {

    public MethodTransformer() { this((x)->true, (x) -> true); }

    public MethodTransformer(Predicate<CtBehavior> targetMethods) { this((x)->true, targetMethods); }

    public MethodTransformer(Predicate<CtClass> targetClasses, Predicate<CtBehavior> targetMethods) {

        if( targetMethods == null) throw new IllegalArgumentException("Method filter can't be null");
        if( targetClasses == null) throw new IllegalArgumentException("Classes filter cant' be null");

        this.targetMethods = targetMethods;
        this.targetClasses = targetClasses;
    }

    protected Predicate<CtBehavior> targetMethods;
    protected Predicate<CtClass> targetClasses;

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer)
        throws IllegalClassFormatException
    {

        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass theClass = pool.get(className.replace('/', '.'));

            if(isClassToSkip(theClass)) {
                return classFileBuffer;
            }

            Arrays.stream(theClass.getDeclaredBehaviors())
                    .filter(this::isValidMethodTarget)
                    .forEach(this::instrument);

            byte[] outputBuffer = theClass.toBytecode();
            theClass.detach();
            return outputBuffer;
        }
        catch(Throwable exc) {
            //TODO: Improve notification
            System.out.println(exc.getMessage());
            return classFileBuffer;
        }
    }

    protected String getEnterProbe(int id) {
        return String.format("{fr.inria.stamp.dissector.StaticDatabase.instance().enter(%d, $args);}", id);
    }

    protected String getExitProbe(int id) {
        return String.format("{fr.inria.stamp.dissector.StaticDatabase.instance().exit(%d);}", id);
    }

    protected void instrument(CtBehavior behavior) {
        try {
            int givenID = StaticDatabase.instance().add(behavior);
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

}
