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


    public static MethodSet fromMutationFile(File mutationFile) throws IOException {
        Set<String> classificationsOfInterest = new HashSet<>(); //TODO: Allow to configure this
        classificationsOfInterest.add("pseudo-tested");
        classificationsOfInterest.add("partially-tested");
        return fromMutationFile(mutationFile, classificationsOfInterest);
    }


    public static MethodSet fromMutationFile(File mutationFile, Set<String> classificationsOfInterest) throws IOException {

        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(new FileReader(mutationFile)).getAsJsonObject();

        Set<String> methods = new HashSet<>();
        Set<String> tests = new HashSet<>();

        for (JsonElement element : root.getAsJsonArray("methods")) {
            JsonObject methodObj = element.getAsJsonObject();



            if(!classificationsOfInterest.contains(methodObj.get("classification").getAsString())) continue; //TODO: Allow to configure this

            String packageName = methodObj.get("package").getAsString().replace("/", ".");
            String className = methodObj.get("class").getAsString();
            String methoName = methodObj.get("name").getAsString();
            String signature = Descriptor.toString(methodObj.get("description").getAsString());
            methods.add(String.format("%s.%s.%s%s", packageName, className, methoName, signature));

            for(JsonElement testNameElement : methodObj.getAsJsonArray("tests")) {

                String name =  getTestSignatureFromTestName(testNameElement.getAsString());
                methods.add(name);
                tests.add(name);
            }

        }

        List<String> methodList = new ArrayList<>(methods.size());
        methodList.addAll(methods);
        return new MethodSet(methodList, tests);
    }

    private static String getTestSignatureFromTestName(String testName) {

        Pattern testWithIndex = Pattern.compile("(?<method>.+)\\[.+\\](?<params>\\(.*\\))");
        Pattern testWithoutIndex = Pattern.compile("(?<method>.+)(?<params>\\(.*\\))");

        Matcher match = testWithIndex.matcher(testName);
        if(!match.matches())
            match = testWithoutIndex.matcher(testName);

        if(!match.matches()) throw new AssertionError("No match for test: " + testName);

        return match.group("method") + "()";



    }

}
