package eu.stamp_project.dissector.agent;

import java.lang.instrument.Instrumentation;
import java.util.stream.Collectors;


public class DissectorAgent {

    private static final int ARGUMENT_ERROR = 1;
    private static final int INPUT_ERROR = 2;
    private static final int UNEXPECTED_ERROR = 3;


    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        ArgsParser args = new ArgsParser();
        args.parse(agentArgs);

        if(!args.isLogPathValid()) {
            System.exit(ARGUMENT_ERROR);
        }
        FileLogger logger = new FileLogger(args.getLogPath());

        if(!args.isInputPathValid()) {
            logger.log("argument error", args.getError());
            System.exit(ARGUMENT_ERROR);
        }

        try {
            logger.logWithTime("Started");
            MethodListParser input = MethodListParser.getParser(args.getInputPath());

            logger.logWithTime("Input parsed");

            if (input.hasErrors()) {
                String message = "Wrong format in lines: " + input.getLinesWithError().stream().map(i -> i.toString()).collect(Collectors.joining(","));
                logger.log("input error", message);
                System.exit(INPUT_ERROR);
            }

            logger.logWithTime("Creating transformer");

            MethodTransformer transformer = new MethodTransformer(input.getTargets());

            transformer.behaviorInstrumented.register( beh -> logger.log("instrumented", beh.getLongName()));
            transformer.transformationError.register( exc -> logger.log("instrumentation error", exc.getMessage()));

            inst.addTransformer(transformer);

        }
        catch (Throwable exc) {
            logger.log("error", exc.getMessage());
            System.exit(UNEXPECTED_ERROR);
        }

        logger.logWithTime("Finished");

    }

}