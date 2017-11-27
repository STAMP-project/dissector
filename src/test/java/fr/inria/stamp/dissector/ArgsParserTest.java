package fr.inria.stamp.dissector;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class ArgsParserTest {


    //Static modifier is enforced by JUnit @BeforeClass annotation
    static File existing, nonExisting;
    static ArgsParser parser;

    @BeforeClass
    //Static modifier is enforced by JUnit @BeforeClass annotation
    public static void createFilesAndParser() throws IOException {
        existing =  File.createTempFile("temp", ".txt");
        nonExisting = File.createTempFile("temp", ".txt");
        nonExisting.delete();//Ensure that the files does not exist.

        parser = new ArgsParser();
    }

    @Test
    public void withFullInput() throws IOException {

        parser.parse(existing.getPath() + ":dir");
        assertFalse("Input was valid but the parser signaled some errors.", parser.hasErrors());
        assertEquals("Unexpected input file", parser.getInputPath(), existing.getPath());
        assertEquals("Unexpected output directory", parser.getOutputPath(), "dir");

    }

    @Test
    public void withoutDirectory() {

        parser.parse(existing.getPath());
        assertFalse("Input was valid but the parser signaled some errors.", parser.hasErrors());
        assertEquals("Unexpected input file", parser.getInputPath(), existing.getPath());
    }


    @Test
    public void withNoInput() {
        assertFalse("Parser failed to detect wrong arguments", parser.parse(""));
        assertTrue("Parser did not store any error.", parser.hasErrors());
    }


}