package fr.inria.stamp.dissector;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

class ArgsParser {
    //NOTE: If the command line arguments grow in compexity use JCommander

    private String inputPath;
    private String outputPath;

    private List<String> errors = new LinkedList<>();

    public boolean parse(String args) {
        return parse(args.split(":"));
    }

    public boolean parse(String... args) {
        errors.clear();

        if(args.length == 0) {
            errors.add("At least an input file should be specified");
            return false;
        }

        inputPath = args[0];
        outputPath = (args.length >= 2)? args[1] : defaultOutput();

        return isInputValid() && isOutputValid();

    }

    protected boolean isInputValid() {
        //It is so tempting to add a list of predicates here :)
        // and some overcomplicated validation mechanism
        File input = new File(inputPath);
        if(!input.exists()) {
            errors.add("Input file " + inputPath + " does not exist.");
            return false;
        }
        if(!input.isFile()) {
            errors.add("Input " + inputPath + " is not a file");
            return false;
        }
        if(!input.canRead()) {
            errors.add("Can not read input file " + inputPath);
            return false;
        }
        return true;
    }

    protected boolean isOutputValid() {
        File output = new File(outputPath);
        if(output.exists()) {

            if(!output.isDirectory()) {
                errors.add("Output path " + outputPath + " is not a directory.");
                return false;
            }

            if(!output.canWrite()) {
                errors.add("Can not write to output directory " + outputPath);
                return false;
            }
        }

        //If the output directory does not exist it will be considered as valid
        return true;
    }

    protected String defaultOutput() {
        return "./dissector-output-" + String.valueOf(System.currentTimeMillis());
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

}
