package eu.stamp_project.dissector.agent;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class Event<T> {

    private List<Consumer<T>>  delegates = new LinkedList<Consumer<T>>();

    public void register(Consumer<T> delegate) {
        if(!delegates.contains(delegate)) {
            delegates.add(delegate);
        }
    }

    public void remove(Consumer<T> delegate) {
        delegates.remove(delegate);
    }

    public void invokeWith(T value) {
        for(Consumer<T> del : delegates)
            del.accept(value);
    }

}
