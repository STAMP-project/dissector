package fr.inria.stamp.dissector;

import javassist.CtBehavior;
import javassist.CtClass;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;


public class StaticDatabase {

    static class Action {

        public int id, type, depth;
        public long thread;

        public Action(int id, int type) {

            Thread current = Thread.currentThread();
            this.id = id;
            this.type = type;
            this.thread = current.getId();
            //Remove two method calls and this constructor
            this.depth = current.getStackTrace().length - 3;
        }
    }

    static class Item {
        public int attributes;
        public String name;

        public Item(int attributes, String name) {
            this.attributes = attributes;
            this.name = name;
        }

        public Item(CtClass aClass) { this(aClass.getModifiers(), aClass.getName()); }

        public Item(CtBehavior behavior) { this(behavior.getModifiers(), behavior.getLongName()); }
    }

    static StaticDatabase db;

    public static StaticDatabase instance() {
        if(db == null)
            db = new StaticDatabase();
        return db;
    }

    private StaticDatabase() {
        initialize();
    }

    //Structures to store transformed and skipped members

   private LinkedList<String> errors;
   private LinkedList<Item> transformed;
   private LinkedList<Action> actions;
   private LinkedList<String[]> parameters;

    //private static int currentID = 0;

    public void initialize() {
       errors = new LinkedList<>();
       transformed = new LinkedList<>();
       actions = new LinkedList<>();
       parameters = new LinkedList<>();
    }

    public synchronized int add(CtBehavior behavior) {
        transformed.add(new Item(behavior));
        return transformed.size() - 1;
    }

    public synchronized void enter(int id, Object[] parameters) {
        action(id, 1);
        String[] values = new String[parameters.length];
        for(int i=0; i< parameters.length; i++)
            values[i] = parameters[i].toString();
        this.parameters.add(values);
    }

    public synchronized void exit(int id) { action(id, 0); }

    public void action(int id, int type) {
       actions.add(new Action(id, type));
    }

    public void error(String message) {
       errors.add(message);
    }

    public void flush(String path) throws IOException {

        File file = new File(path);
        file.mkdir();

        flushItems(path);
        flushCalls(path);
        flushParameters(path);
        flushErrors(path);
    }

    protected void flushErrors(String path) throws IOException{
        try(BufferedWriter writer = Files.newBufferedWriter(Paths.get(path, "errors.txt"));) {
            for(String message: errors) {
                if(message == null)
                    writer.write("[null]");
                else
                    writer.write(message);
                writer.newLine();
            }
        }
    }

    protected void flushItems(String path) throws IOException {
        try(BufferedWriter writer = Files.newBufferedWriter(Paths.get(path, "methods.txt"))) {
            for(Item item: transformed) {
                writer.write(item.name);
                writer.write(":");
                writer.write(String.valueOf(item.attributes));
                writer.newLine();
            }
        }
    }

    protected void flushCalls(String path) throws IOException {
        // String filePath = Paths.get(path, "calls.bin").toString();
        // try(DataOutputStream stream = new DataOutputStream(new FileOutputStream(filePath)))
        // {
        //     for(Action act: actions) {
        //         stream.write(act.id);
        //         stream.write(act.type);
        //         stream.writeLong(act.thread);
        //         stream.write(act.depth);
        //     }
        // }
        // catch(FileNotFoundException exc){
        //     throw new AssertionError("Calls file not found", exc);
        // }

        try(BufferedWriter writer = Files.newBufferedWriter(Paths.get(path, "calls.txt"))) {
            for(Action act: actions) {
                writer.write(String.valueOf(act.id));
                writer.write(":");
                writer.write(String.valueOf(act.type));
                writer.write(":");
                writer.write(String.valueOf(act.thread));
                writer.write(":");
                writer.write(String.valueOf(act.depth));
                writer.newLine();
            }
        }
    }

    protected void flushParameters(String path) throws IOException {
        try(BufferedWriter writer = Files.newBufferedWriter(Paths.get(path, "parameters.txt"))) {
            for(String[] params: parameters) {
                writer.write(String.valueOf(params.length));
                writer.newLine();
                for(String value: params) {
                    writer.write(value);
                    writer.newLine();
                }
            }
        }
    }
}