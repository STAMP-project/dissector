package fr.inria.stamp.dissector;

import javassist.*;
import javassist.bytecode.AccessFlag;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.function.Predicate;


public class MethodTransformer implements ClassFileTransformer {

    private Predicate<CtClass> filter;

    boolean stop = false;

    public MethodTransformer() { this(s -> true); }

    public MethodTransformer(Predicate<CtClass> filter) {
        if(filter == null) throw new NullPointerException("Class filter can not be null");
        this.filter = filter;
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer)
        throws IllegalClassFormatException
    {
//        if(!className.equals("fr/inria/stamp/inspector/test/DummyInputClass")){
//            System.out.println("---> " + className);
//
//            return classFileBuffer;
//
//        }

        //Patch to see if I can get the proper stack trace
        if(stop) return classFileBuffer;
        stop = true;
        System.out.println(className);


        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass theClass = pool.get(className.replace('/', '.'));

            if( isBlackListed(theClass) || isOurs(theClass) || !filter.test(theClass)) {
                StaticDatabase.instance().skip(theClass);
                return classFileBuffer;
            }
            for(CtBehavior behavior : theClass.getDeclaredBehaviors()) {
                if(mustSkip(behavior)) {
                    StaticDatabase.instance().skip(behavior);
                    continue;
                }
                instrument(behavior);
            }

            byte[] outputBuffer = theClass.toBytecode();
            return outputBuffer;
        }
        catch(Throwable exc) {
            //TODO: Improve notification
            System.out.println(exc.getMessage());
            return classFileBuffer;
        }
    }


    private String getEnterProbe(int id) {
        return String.format("{fr.inria.stamp.inspector.StaticDatabase.instance().enter(%d);}", id);
    }

    private String getExitProbe(int id) {
        return String.format("{fr.inria.stamp.inspector.StaticDatabase.instance().exit(%d);}", id);
    }

    private void instrument(CtBehavior behavior) throws CannotCompileException {
        int givenID = StaticDatabase.instance().add(behavior);
        behavior.insertBefore(getEnterProbe(givenID));
        behavior.insertAfter(getExitProbe(givenID), true);

    }

    private boolean mustSkip(CtBehavior  method) {

        int modifiers = method.getModifiers();
        int check = Modifier.ABSTRACT | Modifier.NATIVE | AccessFlag.SYNTHETIC;
        return (modifiers & check) != 0;

    }

    private boolean isOurs(CtClass aClass) { return aClass.getPackageName().equals("fr.inria.stamp.inspector"); }

    //TODO: If the classe filter is set, there is no need for this.
    private boolean isBlackListed(CtClass aClass) {
        return false;
//        String name = aClass.getName();
//        return name.equals("sun.misc.URLClassPath$FileLoader$1") ||
//                name.equals("sun.launcher.LauncherHelper") ||
//                name.equals("sun.launcher.LauncherHelper$FXHelper") ||
//                name.startsWith("java.util");
    }



}
