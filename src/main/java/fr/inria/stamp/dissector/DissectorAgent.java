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
            StaticDatabase.instance().flush();
        
        }));

        //TODO: Set the filter to a given set of methods, infere the classes from there
        InspectorTransformer transformer = new InspectorTransformer();

        inst.addTransformer(transformer);
//
//        inst.addTransformer(new ClassFileTransformer() {
//            @Override
//            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
//                return classfileBuffer;
//            }
//        });

        //System.out.println("HERE");


//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            //System.out.println("HERE");
//        }));
        
    }

}
