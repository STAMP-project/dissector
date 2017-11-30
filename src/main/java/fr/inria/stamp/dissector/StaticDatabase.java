package fr.inria.stamp.dissector;

import javafx.scene.shape.Path;
import javassist.CtBehavior;
import javassist.CtClass;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;


public class StaticDatabase {

    private static BufferedWriter actions, errors, parameters, transformed;

    private static int ID = 0;

    public static void open(String path) throws IOException {

        File dir = new File(path);
        if(!dir.isDirectory()) throw new IllegalArgumentException("Provided path: " + path + "is not a directory.");
        actions = Files.newBufferedWriter(Paths.get(path, "actions.txt"));
        errors = Files.newBufferedWriter(Paths.get(path, "errors.txt"));
        parameters = Files.newBufferedWriter(Paths.get(path, "parameters.txt"));
        transformed = Files.newBufferedWriter(Paths.get(path, "transformed.txt"));
    }

    public final static Event<String> operationError = new Event<>();

    private synchronized static void onOperationError(String message, Object... args) {

        operationError.invokeWith(String.format(message, args));
    }

    public synchronized static int add(CtBehavior method) {

        try {
            transformed.write(String.format("%d:%s:%d\n", ID, method.getLongName(), method.getModifiers()));
        } catch (IOException exc) {
            onOperationError("Unexpected error adding %s. Message: %s", method.getLongName(), exc.getMessage());
        }
        return ID++;

    }

    public static void enter(int id, Object[] values) {
        synchronized (actions) {
            action(id, 1);
            addParameters(values);
        }
    }

    private static void addParameters(Object[] values) {
        try {
            parameters.write(String.valueOf(values.length) + "\n");
            parameters.write(Arrays.stream(values).map(StaticDatabase::escape).collect(Collectors.joining("\n")));
            if(values != null && values.length > 0)
                parameters.write("\n");
        }
        catch(IOException exc) {
            onOperationError("Error reporting parameter values");
        }
    }

    private static String escape(Object input) {
        if(input == null)
            return "[null value]";
        return input.toString()
                .replace("\n", "\\n");
    }

    public static void exit(int id) {
        synchronized (actions) {
            action(id, 0);
        }
    }

    private static void action(int id, int type) {
        try {

            actions.write(String.format("%d:%d:%d:%d\n",
                    id,
                    type,
                    Thread.currentThread().getId(),
                    Thread.currentThread().getStackTrace().length));
        }catch (IOException exc) {
            onOperationError("Error reporting method %s with id %d", (type==0)?"exit":"call", id);
        }
    }

    public synchronized static void error(String message) {
        try {
            errors.write( message + "\n");
        }
        catch(IOException exc) {
            onOperationError("Error reporting instrumentation error: %s", message);
        }
    }

    public static void close() throws IOException {
        actions.close();
        parameters.close();
        transformed.close();
        errors.close();
    }
}