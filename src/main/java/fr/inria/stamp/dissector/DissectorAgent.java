package fr.inria.stamp.dissector;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.function.Predicate;

import fr.inria.stamp.dissector.StaticDatabase;
import javassist.CtBehavior;
import javassist.CtClass;

public class DissectorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                StaticDatabase.instance().flush("output");
            }catch(IOException exc) {
                System.out.println("Could not write results: " + exc.getMessage());
                exc.printStackTrace();
            }
        }));

        //TODO: Set the filter to a given set of methods, infer the classes from there
        Predicate<CtBehavior> filter = (b) ->
            b.getLongName().startsWith("fr.inria.stamp.dissector.test");
        MethodTransformer transformer = new MethodTransformer(filter);
        inst.addTransformer(transformer);
    }

}
