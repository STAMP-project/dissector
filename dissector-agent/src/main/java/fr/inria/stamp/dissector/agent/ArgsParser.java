package fr.inria.stamp.dissector.agent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

class ArgsParser {
    //NOTE: If the command line arguments grow in compexity use JCommander

    private String inputPath;
    private String logPath;

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
        logPath = (args.length >= 2)? args[1] : getDefaultLogPath();

        return isInputValid() && isLogFileValid();

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

    protected boolean isLogFileValid() {
        File log = new File(logPath);
        if(log.exists()) {

            if(!log.isFile()) {
                errors.add("Log path " + logPath + " is not a file.");
                return false;
            }

            if(!log.canWrite()) {
                errors.add("Can not write to log file " + logPath);
                return false;
            }
        }
        else {

            try {
                log.createNewFile();
            }
            catch (IOException exc) {
                errors.add("Can not create log file in: " + logPath + ". Details: " + exc.getMessage());
                return false;
            }
        }
        return true;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getLogPath() {
        return logPath;
    }

    protected String getDefaultLogPath() {
        return "./dissector-" + getLogSuffix()  + ".log";
    }

    public static String getLogSuffix() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat( "yyyyMMddHHmmssSSS");
        return format.format(date);
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

}
