package eu.stamp_project.dissector.agent;
import org.hamcrest.MatcherAssert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ArgumentsParserTest {

    private String inputPath;
    private String logFolderPath;
    private ArgumentsParser parser;

    @Before
    public void createInputFile() throws IOException {
        parser = new ArgumentsParser();
        inputPath = Files.createTempFile("test", "input").toAbsolutePath().toString();
        logFolderPath = Files.createTempDirectory("test").toAbsolutePath().toString();
    }

    @AfterClass
    public static void removeLogFiles() throws IOException {
        Files.list(Paths.get("./"))
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("dissector-") && fileName.endsWith(".log");
                })
                .forEach(path -> path.toFile().delete());
    }

    @Test
    public void testFourValuesSpecified() {
        int port = 19;
        String instrumenter = "invocation";

        parser.parse(
                String.format("%s:%s:%d:%s", inputPath, instrumenter, port, logFolderPath));

        assertFalse("Error detected in correct input", parser.hasErrors());
        assertEquals("Unexpected input file path", parser.getInputFile().getAbsolutePath(), inputPath);
        MatcherAssert.assertThat(parser.getInstrumenter(), is(instanceOf(InvocationInstrumenter.class)));
        assertEquals("Wrong port", parser.getPort(), port);
        assertTrue("Parser should be able to log", parser.canLog());
    }

    @Test
    public void testOnlyOneValue() {
        parser.parse(inputPath);
        assertFalse("Error detected in correct input", parser.hasErrors());
        assertEquals("Unexpected input file path", parser.getInputFile().getAbsolutePath(), inputPath);
        MatcherAssert.assertThat(parser.getInstrumenter(), is(instanceOf(InvocationInstrumenter.class)));
        assertEquals("Should have specified the default port", ArgumentsParser.DEFAULT_PORT, parser.getPort());
        assertTrue("Parser should be able to log", parser.canLog());

    }

    @Test
    public void testInputFileDoesNotExist() throws IOException {
        Files.delete(Paths.get(inputPath));
        parser.parse(inputPath);
        assertTrue("Invalid input file", parser.hasErrors());
    }

    @Test
    public void testInputNotFile() {
        parser.parse(logFolderPath);
        assertTrue("Folder as input file", parser.hasErrors());
    }

    @Test
    public void testWrongPort() {
        parser.parse(inputPath + "::abcd");
        assertTrue("No error in wrong input", parser.hasErrors());
        assertThat(parser.getError(), startsWith("Invalid port value."));
    }

    @Test
    public void testInstrumentationDoesNotExist() {
        parser.parse(inputPath + ":abcd");
        assertTrue("No error in wrong instrumentation", parser.hasErrors());
        assertThat(parser.getError(), endsWith("does not exist"));
    }

    @Test
    public void testStackInstrumentation() {
        parser.parse(inputPath + ":stack");
        assertThat(parser.getInstrumenter(), instanceOf(StackInstrumenter.class));
    }

}