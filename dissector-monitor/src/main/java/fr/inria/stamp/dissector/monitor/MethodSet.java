package fr.inria.stamp.dissector.monitor;

import com.google.gson.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.bytecode.Descriptor;


public class MethodSet {

    public MethodSet(List<String> methods, Set<String> tests) {
        this.methods = methods;
        this.tests = new boolean[methods.size()];
        for(int i = 0; i < methods.size(); i++)
            this.tests[i] = tests.contains(methods.get(i));
    }

    private List<String> methods;

    private boolean[] tests;

    public boolean isValid(int id) {
        return id >= 0 && id < methods.size();
    }

    public boolean isTest(int id) {
        return tests[id];
    }

    public boolean isTest(String name) {
        int index = methods.indexOf(name);
        return index >= 0 && tests[index];
    }

    public String getName(int id) {
        return methods.get(id);
    }

    public int size() {
        return methods.size();
    }

    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    public void save(File output) throws IOException {

        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output))) {

            for (String method : methods) {
                bufferedWriter.write(method);
                bufferedWriter.newLine();
            }
        }

    }
}
