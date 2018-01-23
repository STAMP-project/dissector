package fr.inria.stamp.dissector.monitor;

import java.util.*;

public class ProcessEmulator {

    private StackEmulator[] threads;

    private MethodSet methods;

    public ProcessEmulator(MethodSet methods) { this(5, methods); }

    public ProcessEmulator(int threads, MethodSet methods) {
        this.threads = new StackEmulator[threads];
        this.methods = methods;
    }

    public void enter(int thread, int methodID, int stackDepth) {

        if(threads[thread] == null)
            threads[thread] = new StackEmulator(methods);

        threads[thread].enter(methodID, stackDepth);
        corrupt = corrupt || threads[thread].isCorrupt();
    }

    public void exit(int thread, int methodID, int stackDepth) {
        if(threads[thread] == null) {
            corrupt = true;
            return;
        }
        threads[thread].exit(methodID, stackDepth);
        corrupt = corrupt || threads[thread].isCorrupt();
    }

    private boolean corrupt = false;

    public boolean isCorrupt() {
        return corrupt;
    }


    private Set<StackEmulator.StackDistance> getAllDistances() {
        Set<StackEmulator.StackDistance> allDists = new HashSet<>();
        for(StackEmulator thread : threads) {
            if(thread != null)
                allDists.addAll(thread.getDistances());
        }

        return allDists;
    }

    private StackEmulator.StackDistance[] getSortedDistances(Collection<StackEmulator.StackDistance> distances) {

        StackEmulator.StackDistance[] result = new StackEmulator.StackDistance[distances.size()];

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
