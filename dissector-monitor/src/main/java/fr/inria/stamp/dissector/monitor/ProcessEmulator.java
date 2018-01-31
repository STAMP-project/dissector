package fr.inria.stamp.dissector.monitor;

import java.util.*;

public class ProcessEmulator {

    private Map<Integer, StackEmulator> threads;

    private MethodSet methods;


    public ProcessEmulator(MethodSet methods) {

        this.threads = new HashMap<>();
        this.methods = methods;
    }

    public void enter(int thread, int methodID, int stackDepth) {
        StackEmulator emulator = threads.computeIfAbsent(thread, th -> new StackEmulator(methods));
        if(emulator.isCorrupt()) return;
        emulator.enter(methodID, stackDepth);
        corrupt = corrupt || emulator.isCorrupt();
    }

    public void exit(int thread, int methodID, int stackDepth) {

        if(!threads.containsKey(thread)) {
            corrupt = true;
            return;
        }
        StackEmulator emulator = threads.get(thread);
        emulator.exit(methodID, stackDepth);
        corrupt = corrupt || emulator.isCorrupt();
    }

    private boolean corrupt = false;

    public boolean isCorrupt() {
        return corrupt;
    }


    private Set<StackEmulator.StackDistance> getAllDistances() {
        Set<StackEmulator.StackDistance> allDists = new HashSet<>();
        for(StackEmulator thread : threads.values()) {
            if(thread != null)
                allDists.addAll(thread.getDistances());
        }

        return allDists;
    }

    private StackEmulator.StackDistance[] getSortedDistances(Collection<StackEmulator.StackDistance> distances) {

        StackEmulator.StackDistance[] result = new StackEmulator.StackDistance[distances.size()];
        distances.toArray(result);

        Arrays.<StackEmulator.StackDistance>sort(result, (sd1, sd2) -> {
            int cmp = sd1.method - sd2.method;
            if( cmp != 0)
                return cmp;
            cmp = sd1.test - sd2.test;
            if(cmp != 0)
                return cmp;
            return sd1.distance - sd2.distance;
        } );

        return  result;
    }


    public List<MethodEntry> getReport() {

        StackEmulator.StackDistance[] distances = getSortedDistances(getAllDistances());
        List<MethodEntry> report = new LinkedList<>();

        if(distances.length == 0) return report;

        StackEmulator.StackDistance dist = distances[0];

        MethodEntry methodEntry = new MethodEntry(methods.getName(dist.method));
        TestEntry testEntry = new TestEntry(methods.getName(dist.test), dist.distance);
        methodEntry.getTests().add(testEntry);
        report.add(methodEntry);

        for(int i = 1; i < distances.length; i++) {

            if(dist.method != distances[i].method) {
                dist = distances[i];
                methodEntry = new MethodEntry(methods.getName(dist.method));
                testEntry = new TestEntry(methods.getName(dist.test), dist.distance);
                methodEntry.getTests().add(testEntry);
                report.add(methodEntry);
            }
            else if(dist.test != distances[i].test) {
                dist = distances[i];
                testEntry = new TestEntry(methods.getName(dist.test), dist.distance);
                methodEntry.getTests().add(testEntry);
            }
            else
            {
                testEntry.getDistances().add(distances[i].distance);
            }
        }

        return report;

    }

}
