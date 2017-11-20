package fr.inria.stamp.dissector;

import javassist.CtBehavior;
import javassist.CtClass;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;


public class StaticDatabase {

    static class Action {

        public int id, type;
        public long thread;

        public Action(int id, long thread, int type) {
            this.id = id;
            this.thread = thread;
            this.type = type;
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
   private LinkedList<Item> skipped;
   private LinkedList<Item> transformed;
   private LinkedList<Action> actions;

    //private static int currentID = 0;

    private int ACTION_ENTER = 1;
    private int ACTION_EXIT = 0;

    public void initialize() {
       errors = new LinkedList<>();
       skipped = new LinkedList<>();
       transformed = new LinkedList<>();
       actions = new LinkedList<>();
    }

    public synchronized int add(CtBehavior behavior) {
        transformed.add(new Item(behavior));
        return transformed.size() - 1;
    }

    public synchronized void skip(CtBehavior behavior) {
       skipped.add(new Item(behavior));
    }

    public synchronized void skip(CtClass aClass) {
       skipped.add(new Item(aClass));
    }

    public synchronized void action(int id, int type) {
       actions.add(new Action(id, Thread.currentThread().getId(), type));
    }

    public void enter(int id) { action(id, ACTION_ENTER); }

    public void exit(int id) { action(id, ACTION_EXIT); }

    public void error(String className) {
       if(errors == null) initialize();
       errors.add(className);
    }

    public void flush(String path) {
        //TODO: Instead of writing to a text file consider storing the information in a sqlite file
       try {
           BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
           writer.write(String.valueOf(errors.size()));
           writer.close();

       }catch(Throwable exc) {
           exc.printStackTrace();
       }

    }
}