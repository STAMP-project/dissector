package fr.inria.stamp.dissector.agent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Paths;
import java.util.stream.Collectors;




public class DissectorAgent {

    private static final int ARGUMENT_ERROR = 1;
    private static final int INPUT_ERROR = 2;
    private static final int UNEXPECTED_ERROR = 3;


    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        //Agent arguments
        ArgsParser args = new ArgsParser();
        args.parse(agentArgs);

        if (args.hasErrors()) {

            for (String message : args.getErrors())
                System.err.println(message);

            System.exit(ARGUMENT_ERROR); //Errors in arguments
        }

        final FileLogger logger = new FileLogger(Paths.get(args.getLogPath()));

        try {
            MethodListParser input = MethodListParser.getParser(args.getInputPath());
            if (input.hasErrors()) {
                String message = "Input error in lines: " + input.getLinesWithError().stream().map(i -> i.toString()).collect(Collectors.joining(","));
                logger.log(message);
                System.err.println(message);
                System.exit(INPUT_ERROR);
            }
            inst.addTransformer(new MethodTransformer(input.getMethods(), input.getClasses()));
        } catch (Throwable exc) {
            String message = "Error: " + exc.getMessage();
            logger.log(message);
            System.err.println(message);
            System.exit(UNEXPECTED_ERROR);
        }


    }

}