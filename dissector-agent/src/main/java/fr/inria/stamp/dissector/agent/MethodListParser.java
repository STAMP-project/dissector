package fr.inria.stamp.dissector.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MethodListParser {

    private static Pattern SIGNATURE;

    static {
        //Build the pattern

        String identifier = "[0-9a-zA-Z_\\$]+";
        String qualifiedName = String.format("%1$s(\\.%1$s)*", identifier);
        String type = qualifiedName + "(\\[\\])*";
        String parameters = String.format("\\((%1$s(,%1$s)*)?\\)", type);
        String comments = "\\s*:.*";
        String signature = String.format("^(?<method>(?<class>%s)\\.%s%s)(%s)?$", qualifiedName, identifier, parameters, comments);

        SIGNATURE = Pattern.compile(signature);
    }

    private List<String> methods = new LinkedList<>();
    private Set<String> classes = new HashSet<>();
    private LinkedList<Integer> errors = new LinkedList<>();


    public boolean parse(Stream<String> input) {
        reset();

        final AtomicInteger position = new AtomicInteger(1);
        input.forEach((str) -> {

            Matcher matcher = SIGNATURE.matcher(str);
            if(matcher.matches()) {
                methods.add(matcher.group("method"));
                classes.add(matcher.group("class"));
            }
            else {
                errors.add(position.get());
            }
            position.incrementAndGet();
        });
        return !hasErrors();
    }

    public static MethodListParser getParser(String path) throws IOException {
        MethodListParser parser = new MethodListParser();
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            parser.parse(reader.lines());
            return parser;
        }
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

    protected void reset() {
        methods.clear();
        classes.clear();
        errors.clear();
    }

    public List<String> getMethods() {
        return methods;
    }

    public Set<String> getClasses() {
        return classes;
    }

    public boolean hasTargets() {
        return methods.size() > 0 || classes.size() > 0;
    }

    public List<Integer> getLinesWithError() {
        return errors;
    }

}
