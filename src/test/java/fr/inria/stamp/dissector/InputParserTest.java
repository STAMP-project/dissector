package fr.inria.stamp.dissector;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.inria.stamp.dissector.matchers.EqualSetMatcher.equalSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InputParserTest {


    @Test
    public void withCorrectInput() {
        String[] input = {
                "path.to.package.Class.method():Some comments",
                "path.to.package.Class.method(int):2",
                "path.to.package.Class.method(int)",
                "path.to.package.Class.method(int,boolean):2",
                "path.to.package.Class.method(int[])",
                "path.to.package.Class.method(org.java.util.Set,int[][])"
        };

        Set<String> classes = new HashSet<>(Arrays.asList("path.to.package.Class"));

        Set<String> methods = new HashSet<>(Arrays.asList(
                "path.to.package.Class.method()",
                "path.to.package.Class.method(int)",
                "path.to.package.Class.method(int)",
                "path.to.package.Class.method(int,boolean)",
                "path.to.package.Class.method(int[])",
                "path.to.package.Class.method(org.java.util.Set,int[][])"
        ));

        InputParser parser = new InputParser();

        parser.parse(Arrays.stream(input));

        String wrongInputs = parser.getLinesWithError()
                .stream()
                .map((i) -> input[i-1] )
                .collect(Collectors.joining("\n"));

        assertFalse("Unexpected errors found parsing the following inputs:\n" + wrongInputs,
                parser.hasErrors());

        assertThat(parser.getClasses(), equalSet(classes));
        assertThat(parser.getMethods(), equalSet(methods));
    }

    @Test
    public void withIncorrectOutput() {
        String[] input = {
                ".method",
                ": something in here",
                "method(some)"
        };

        InputParser parser = new InputParser();

        assertFalse("Parser found no error", parser.parse(Arrays.stream(input)));

        boolean[] error = new boolean[input.length];
        for(int pos : parser.getLinesWithError())
            error[pos - 1] = true;

        String wrongInputs = IntStream.range(0, error.length)
                .filter((i) -> !error[i])
                .mapToObj((i) -> input[i])
                .collect(Collectors.joining("\n"));

        assertEquals("The following lines were parsed by error:\n" + wrongInputs,
                parser.getLinesWithError().size(), input.length);
    }

}