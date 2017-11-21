package fr.inria.stamp.dissector;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

        //TODO: Args validation
        Set<String> methods = loadMethods(agentArgs);
        MethodTransformer transformer = new MethodTransformer(
                /*getClassPredicate(methods),*/
                (aClass) -> true,
                getMethodPredicate(methods));
        inst.addTransformer(transformer);
    }

    protected static Set<String> loadMethods(String path) {
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(path))){
            return reader.lines()
                    .map((str) ->  str.substring(0, str.indexOf(':')))
                    .collect(Collectors.toSet());
        }catch (IOException exc) {
            //TODO: Improve
            exc.printStackTrace();
            throw new AssertionError("Failed to load the input file");
        }
    }

    protected static Predicate<CtBehavior> getMethodPredicate(Set<String> methods) {
        return (method) -> methods.contains(method.getLongName());
    }

    protected static Predicate<CtClass> getClassPredicate(Set<String> methods) {
        //FIXME: Should be the last index of . before (
        Set<String> classes = methods.stream()
                                        .map((str) -> str.substring(str.indexOf('(')))
                                        .collect(Collectors.toSet());

        return (aClass) -> {
            System.out.println(aClass.getName());
           return classes.contains(aClass.getName());
        };

    }

}
