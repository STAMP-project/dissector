package eu.stamp_project.dissector.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MethodListParser {

    private static Pattern SIGNATURE;

    static {
        //Build the pattern
        String identifier = "[^\\.;\\[/\\(\\):]+";
        String qualifiedName = String.format("%1$s(/%1$s)*", identifier);
        String signature = String.format("(?<class>%s)/(?<method>%s)(?<desc>\\(.*\\).+)", qualifiedName, identifier);
        SIGNATURE = Pattern.compile(signature);

        //SIGNATURE = Pattern.compile("(?<class>.+)/(?<method>.+)(?<desc>\\(\\)V)");
    }

    private Map<String, Set<TargetMethod>> targets = new HashMap<>();
    private LinkedList<Integer> errors = new LinkedList<>();


    public boolean parse(Stream<String> input) {

        final AtomicInteger position = new AtomicInteger();
        input.forEach((str) -> {

            Matcher matcher = SIGNATURE.matcher(str);
            if(matcher.matches()) {

                String className = matcher.group("class");

                //Don't set our classes as targets
                if(className.startsWith("fr/inria/stamp/dissector")) return;

                Set<TargetMethod> container = targets.get(className);

                if(container == null) {
                    container = new HashSet<>();
                    targets.put(className, container);
                }

                TargetMethod method = new TargetMethod(className, matcher.group("method"), matcher.group("desc"), position.get());
                container.add(method);
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

    public Map<String, Set<TargetMethod>> getTargets() {
        return targets;
    }

    public List<Integer> getLinesWithError() {
        return errors;
    }

}
