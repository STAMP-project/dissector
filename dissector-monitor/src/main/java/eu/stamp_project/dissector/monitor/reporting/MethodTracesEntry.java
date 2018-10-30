package eu.stamp_project.dissector.monitor.reporting;

import java.util.LinkedList;
import java.util.List;

public class MethodTracesEntry {

    private String method;

    private List<StackTraceElement[]> traces = new LinkedList<>();

    public MethodTracesEntry(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void addTrace(StackTraceElement[] trace) {
        if(trace == null || trace.length == 0)
            throw new IllegalArgumentException("Stack trace should not be null and contain more than one element");
        traces.add(trace);
    }

    public List<StackTraceElement[]> getTraces() {
        return traces;
    }
}
