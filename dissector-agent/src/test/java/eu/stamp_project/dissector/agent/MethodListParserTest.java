package eu.stamp_project.dissector.agent;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;

public class MethodListParserTest {

    @Test
    public void testRegularInputList() {

        String[] input = {
                "path/to/Class/method()V",
                "path/to/AnotherClass/method(II)Ljava/lang/String;",
        };

        MethodListParser parser = new MethodListParser();
        parser.parse(Arrays.stream(input));
        assertFalse(parser.hasErrors());
    }

}