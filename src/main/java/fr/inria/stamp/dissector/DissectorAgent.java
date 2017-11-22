package fr.inria.stamp.dissector;

import javassist.CtBehavior;
import javassist.CtClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Predicate;

public class DissectorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        ArgsParser argsParser = new ArgsParser();
        argsParser.parse(agentArgs);
        if(argsParser.hasErrors()) {
            for(String msg: argsParser.getErrors())
                System.err.println();
            System.exit(1);
        }

        InputParser inputParser = new InputParser();

        try(BufferedReader reader = Files.newBufferedReader(Paths.get(argsParser.getInputPath()))) {

            inputParser.parse(reader.lines());
            if(inputParser.hasErrors()) {
                System.err.print("Errors were found while processing the input file at lines:");
                for(int line: inputParser.getLinesWithError()) {
                    System.err.print(" ");
                    System.err.print(line);
                }
                System.err.println(".");
                System.exit(2);
            }

            if(!inputParser.hasTargets()) {
                System.err.println("No target found on input file.");
                System.exit(2);
            }

        }
        catch (IOException exc) {
            System.err.println("Unexpected error while opening the input file: " + exc.getMessage());
            System.exit(1);
        }


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                StaticDatabase.instance().flush("output");
            } catch(IOException exc) {
                System.err.println("Unexpected error while writing the output files: " + exc.getMessage());
                System.exit(3);
            }
        }));

        instrument(inputParser.getMethods(), inputParser.getClasses(), inst);
    }

    protected static void instrument(Set<String> methods, Set<String> classes, Instrumentation inst) {
        inst.addTransformer( new MethodTransformer(
                getClassPredicate(classes),
                getMethodPredicate(methods)
        ));
    }

    protected static Predicate<CtBehavior> getMethodPredicate(Set<String> methods) {
        if(methods.size() == 0)
            return (m) -> true;
        return (m) -> methods.contains(m.getLongName());
    }

    protected static Predicate<CtClass> getClassPredicate(Set<String> classes) {
        if(classes.size() == 0) {
            return (c) -> true;
        }
        return (c) -> classes.contains(c.getName());

    }



}
