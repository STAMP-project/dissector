package fr.inria.stamp.dissector.monitor;

import com.google.gson.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public String getName(int id) {
        return methods.get(id);
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

        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(new FileReader(mutationFile)).getAsJsonObject();

        Set<String> methods = new HashSet<>();
        Set<String> tests = new HashSet<>();

        for (JsonElement element : root.getAsJsonArray("methods")) {
            JsonObject methodObj = element.getAsJsonObject();

            if(methodObj.get("classification").getAsString().equals("tested")) continue; //TODO: Allow to configure this

            String packageName = methodObj.get("package").getAsString().replace("/", ".");
            String className = methodObj.get("class").getAsString();
            String methoName = methodObj.get("name").getAsString();
            String signature = Descriptor.toString(methodObj.get("description").getAsString());
            methods.add(String.format("%s.%s.%s%s", packageName, className, methoName, signature));

            for(JsonElement testNameElement : methodObj.get("tests").getAsJsonObject().getAsJsonArray("ordered")) {

                String name = testNameElement.getAsString();
                methods.add(name);
                tests.add(name);
            }

        }

        List<String> methodList = new ArrayList<>(methods.size());
        methodList.addAll(methods);
        return new MethodSet(methodList, tests);
    }

}
