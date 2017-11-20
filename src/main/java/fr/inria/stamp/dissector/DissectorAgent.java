package fr.inria.stamp.dissector;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import fr.inria.stamp.dissector.StaticDatabase;

public class DissectorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);


    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            StaticDatabase.instance().flush("output.txt"); //TODO: Change
        
        }));

        //TODO: Set the filter to a given set of methods, infere the classes from there
        MethodTransformer transformer = new MethodTransformer();

        inst.addTransformer(transformer);
    }

}
