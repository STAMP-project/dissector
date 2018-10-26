package eu.stamp_project.dissector.agent;

import javassist.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


public class DissectorAgent {

    private static final int ARGUMENT_ERROR = 1;
    private static final int INPUT_ERROR = 2;
    private static final int UNEXPECTED_ERROR = 3;


    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {

        ArgumentsParser parser = new ArgumentsParser();
        parser.parse(agentArgs);

        if(!parser.canLog()) {
            System.exit(ARGUMENT_ERROR);
        }

        FileLogger logger = parser.getFileLogger();

        if(parser.hasErrors()) {
            logger.log("argument error", parser.getError());
            System.exit(ARGUMENT_ERROR);
        }

        try {

            logger.logWithTime("Started");
            MethodListParser input = MethodListParser.getParser(parser.getInputFile());

            logger.logWithTime("Input parsed");

            if (input.hasErrors()) {
                String message = "Wrong format in lines: " + input.getLinesWithError().stream().map(i -> i.toString()).collect(Collectors.joining(","));
                logger.log("input error", message);
                System.exit(INPUT_ERROR);
            }

            logger.logWithTime("Creating transformer");

            MethodTransformer transformer = new MethodTransformer(input.getTargets(), parser.getInstrumenter());

            transformer.behaviorInstrumented.register( beh -> logger.log("instrumented", beh.getLongName()));
            transformer.transformationError.register( exc -> logger.log("instrumentation error", exc.getMessage()));

            inst.addTransformer(transformer);

            logger.logWithTime("Attaching the tracer jar");
            JarFile jar = generateTracerJar(0);

            logger.log("Probe sender JAR generated at " + jar.getName());
            inst.appendToSystemClassLoaderSearch(jar);

        }
        catch (Throwable exc) {
            logger.log("error", exc.getMessage());
            System.exit(UNEXPECTED_ERROR);
        }

        logger.logWithTime("Finished");

    }

    public static JarFile generateTracerJar(int port) throws IOException {
        return generateJarFile(generateTracerClass(port));
    }

    public static CtClass generateTracerClass(int port) {
        try {

            ClassPool pool = ClassPool.getDefault();
            CtClass tracerClass = pool.makeClass("eu.stamp_project.instrumentation.CallTracer");
            CtMethod sendMethod = CtMethod.make("public static void send(java.lang.String message){System.err.println(\"\\n[[D][\" + message + \"]]\");}", tracerClass);
            tracerClass.addMethod(sendMethod);
            return tracerClass;
        }
        catch (CannotCompileException exc) { // This should not happen
            throw new AssertionError("CallTracer instrumentation could not be compiled. Details: " + exc.getReason());
        }

    }

    public static JarFile generateJarFile(CtClass tracerClass) throws IOException {

        try {

            File tempFile = File.createTempFile("__stamp__", "__intrumentation__");
            FileOutputStream fileStream = new FileOutputStream(tempFile);
            JarOutputStream jarStream = new JarOutputStream(fileStream);

            jarStream.putNextEntry(new ZipEntry(tracerClass.getPackageName().replace(".", "/") + "/"));
            jarStream.putNextEntry(new ZipEntry(tracerClass.getName().replace(".", "/") + ".class"));

            jarStream.write(tracerClass.toBytecode());

            jarStream.closeEntry();
            jarStream.close();

            fileStream.close();

            return new JarFile(tempFile);
        }
        catch (CannotCompileException exc) { //This should not happen
            throw new AssertionError("Could not convert CallTracer class to bytecode. Details: " + exc.getReason());
        }


    }

}