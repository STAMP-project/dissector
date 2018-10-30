package eu.stamp_project.dissector.monitor.reporting;

import java.util.ArrayList;
import java.util.List;

public class TestEntry {

    public TestEntry(String test) {
        this.test = test;
        distances = new ArrayList<>();
    }

    public TestEntry(String test, int distance) {
        this(test);
        distances.add(distance);
    }

    private String test;

    public String getTest() {
        return test;
    }

    public List<Integer> getDistances() {
        return distances;
    }

    private List<Integer> distances;

}
