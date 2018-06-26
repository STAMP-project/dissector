package eu.stamp_project.dissector.agent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class ArgsParser {
    //NOTE: If the command line arguments grow in compexity use JCommander

    private String inputPath;
    private String logPath;

    private String error = "";

    public boolean parse(String args) {
        return parse(args.split(":"));
    }

    public boolean parse(String... args) {
        error = "";

        if(args.length == 0) {
            setError("At least an input file should be specified");
            return false;
        }

        inputPath = args[0];
        logPath = (args.length >= 2)? args[1] : getDefaultLogPath();

        return  (validInput = parseInputPath()) &&  (validLog = parseLogPath());

    }

    private boolean parseInputPath() {
        //It is so tempting to add a list of predicates here :)
        // and some overcomplicated validation mechanism
        File input = new File(inputPath);
        if(!input.exists()) {
            setError("Input file " + inputPath + " does not exist.");
            return false;
        }
        if(!input.isFile()) {
            setError("Input " + inputPath + " is not a file");
            return false;
        }
        if(!input.canRead()) {
            setError("Can not read input file " + inputPath);
            return false;
        }
        return true;
    }

    private boolean parseLogPath() {
        File log = new File(logPath);
        if(log.exists()) {

            if(!log.isFile()) {
                setError("Log path " + logPath + " is not a file.");
                return false;
            }

            if(!log.canWrite()) {
                setError("Can not write to log file " + logPath);
                return false;
            }
        }
        else {

            try {
                log.createNewFile();
            }
            catch (IOException exc) {
                setError("Can not create log file in: " + logPath + ". Details: " + exc.getMessage());
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

    private boolean validInput = false;
    public boolean isInputPathValid() {
        return validInput;

    }

    private boolean validLog = false;
    public boolean isLogPathValid() {
        return validLog;
    }

    protected String getDefaultLogPath() {
        return "./dissector-" + getLogSuffix()  + ".log";
    }

    public static String getLogSuffix() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat( "yyyyMMddHHmmssSSS");
        return format.format(date);
    }

    public String getError() {
        return error;
    }

    private void setError(String error) {
        this.error = error;
    }

    public boolean hasErrors() {
        return !isInputPathValid() || !isLogPathValid();
    }

}
