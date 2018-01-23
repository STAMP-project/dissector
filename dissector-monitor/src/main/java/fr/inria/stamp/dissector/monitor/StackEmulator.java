package fr.inria.stamp.dissector.monitor;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class StackEmulator {

    static class ActivationRecord {

        public ActivationRecord(int method, int depth) {
            this.method = method;
            this.depth = depth;
        }

        public int method;
        public int depth;

        public StackDistance distanceTo(ActivationRecord record) {
            return new StackDistance(this.method, record.method, depth - record.depth);
        }

        public boolean isTheSame(int method, int depth) {
            return this.method == method && this.depth == depth;
        }

        public int hasCode() {
            return 37 * (37 * 13 + method) + depth;
        }

        public boolean equals(Object other) {
            if(!(other instanceof  ActivationRecord)) return false;
            ActivationRecord or = (ActivationRecord) other;
            return method == or.method && depth == or.depth;
        }

    }

    static class StackDistance {

        public int test;
        public int method;
        public int distance;

        public StackDistance(int method, int test, int distance) {
            this.method = method;
            this.test = test;
            this.distance = distance;
        }

        public int hasCode() {
            return 37 * (37 * (37 * 13 + method) + test) + distance;
        }

        public boolean equals(Object other) {
            if(!(other instanceof  StackDistance)) return false;
            StackDistance od = (StackDistance) other;
            return method == od.method && test == od.test && od.distance == distance;
        }
    }



    public StackEmulator(MethodSet methods) {
        this.methods = methods;
    }

    private MethodSet methods;

    private Stack<ActivationRecord> fullStack = new Stack<>();

    private Stack<ActivationRecord> testStack = new Stack<>();

    private Set<StackDistance> distances = new HashSet<>();

    public Set<StackDistance> getDistances() {
        return distances;
    }

    public void enter(int methodID, int stackDepth) {
        if(corrupt) return;

        if(!fullStack.isEmpty() && fullStack.peek().depth >= stackDepth) {
            corrupt = true;
            return;
        }

        ActivationRecord record = new ActivationRecord(methodID, stackDepth);
        fullStack.push(record);

        if(methods.isTest(methodID)) {
            testStack.push(record);
        }
        else if (!testStack.isEmpty()) {
            distances.add(record.distanceTo(testStack.peek()));
        }
    }

    private boolean couldNotPop(Stack<ActivationRecord> stack, int methodID, int depth) {
        if(stack.isEmpty() && !stack.peek().isTheSame(methodID, depth))
            return true;
        stack.pop();
        return false;
    }

    public void exit(int methodID, int stackDepth) {
        if(corrupt) return;

        corrupt = couldNotPop(fullStack, methodID, stackDepth)
                || methods.isTest(methodID) && couldNotPop(testStack, methodID, stackDepth);
    }

    private boolean corrupt = false;

    public boolean isCorrupt() {
        return corrupt;
    }

}
