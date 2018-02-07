package fr.inria.stamp.dissector.agent;

import java.util.HashSet;
import java.util.Set;

public class TargetClass {

    private String name;

    public String getName() {
        return name;
    }

    public TargetClass(String name) {
        this.name = name;
        methods = new HashSet<>();
    }

    private Set<TargetMethod> methods;

    public Set<TargetMethod> getMethods() {
        return methods;
    }

    public void addMethod(TargetMethod method) {
        methods.add(method);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && obj instanceof TargetClass && ((TargetClass)obj).name.equals(name);
    }

}
