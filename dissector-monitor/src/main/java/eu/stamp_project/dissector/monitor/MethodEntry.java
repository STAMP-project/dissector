package eu.stamp_project.dissector.monitor;

import java.util.ArrayList;
import java.util.List;

public class MethodEntry {

    public MethodEntry(String name) {
        method = name;
        tests = new ArrayList<>();
    }

    private String method;

    public String getMethod() {
        return method;
    }

    private List<TestEntry> tests;

    public List<TestEntry> getTests() {
        return tests;
    }

}
