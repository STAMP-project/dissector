package fr.inria.stamp.dissector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;


public class DissectorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        ArgsParser args = parseArguments(agentArgs);

        //Create the folder
        new File(args.getOutputPath()).mkdirs();

        //Load list of target methods
        final FileLogger logger = new FileLogger(Paths.get(args.getOutputPath(), "dissector.log"));
        logger.log(agentArgs);

        InputParser input = parseInput(args.getInputPath(), logger);

        //Save results
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                StaticDatabase.instance().flush(args.getOutputPath());
            } catch(IOException exc) {
                logger.log("Unexpected error while writing the output files: " + exc.getMessage());
                System.exit(3);
            }
        }));

        instrument(inst, input);

    }

    private static void instrument(Instrumentation inst, InputParser input) {
        MethodTransformer transformer = new MethodTransformer(
                input.getClasses(),
                input.getMethods());

        transformer.instrumentationError().register(
                exc ->
                    StaticDatabase.instance().error(
                    exc.getClass().getName() + ":\n" +
                    //Some expections have null messages
                    (exc.getMessage() == null ? "":exc.getMessage()) + "\n\t" +
                    Arrays.stream(exc.getStackTrace())
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining("\n\t"))));

        inst.addTransformer(transformer);
    }

    private static InputParser parseInput(String inputPath, FileLogger logger) {
        InputParser inputParser = new InputParser();
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(inputPath))) {

            inputParser.parse(reader.lines());
            if(inputParser.hasErrors()) {
               logger.log(getParserError(inputParser));
               System.exit(2);
            }

            if(!inputParser.hasTargets()) {
                logger.log("No target found on input file.");
                System.exit(2);
            }

        }
        catch (IOException exc) {
            logger.log("Unexpected error while opening the input file: " + exc.getMessage());
            System.exit(1);
        }
        return inputParser;
    }

    private static ArgsParser parseArguments(String agentArgs) {
        //Get input file and output folder
        ArgsParser argsParser = new ArgsParser();
        argsParser.parse(agentArgs);
        if(argsParser.hasErrors()) {

            FileLogger logger = new FileLogger("dissector.log");
            logger.log("Some errors were found while inspecting the given arguments:");
            for(String msg: argsParser.getErrors())
                logger.log(msg);
            System.exit(1);
        }
        return argsParser;
    }


    private static String getParserError(InputParser parser) {
        return "Errors were found while processing the input file at lines: " +
        parser.getLinesWithError().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + ".";
    }


}
