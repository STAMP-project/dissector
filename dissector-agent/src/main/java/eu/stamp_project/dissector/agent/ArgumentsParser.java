package eu.stamp_project.dissector.agent;



import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArgumentsParser {
    // Favor a tailored parser to keep the agent jar as small as possible
    // and to avoid license conflicts/

    static final int DEFAULT_PORT = 2112;

    private InputFileParameter inputFileParameter = new InputFileParameter();
    private InstrumenterParameter instrumenterParameter = new InstrumenterParameter();
    private PortParameter portParameter = new PortParameter(DEFAULT_PORT);
    private FileLoggerParameter fileLoggerParameter = new FileLoggerParameter();

    private Parameter[] params = {inputFileParameter, instrumenterParameter, portParameter, fileLoggerParameter};

    public FileLogger getFileLogger() {
        return fileLoggerParameter.get();
    }

    public int getPort() {
        return portParameter.get();
    }

    public File getInputFile() {
        return inputFileParameter.get();
    }

    public BehaviorInstrumenter getInstrumenter() {
        return instrumenterParameter.get();
    }

    public boolean canLog() {
        return fileLoggerParameter.isValid();
    }

    public boolean hasErrors() {
        for(Parameter parameter : params) {
            if(!parameter.isValid())
                return true;
        }
        return false;
    }

    public String getError() {
        for(Parameter parameter: params) {
            if(!parameter.isValid())
                return parameter.getError();
        }
        return null;
    }

    public boolean parse(String args) { return parse(args.split(":")); }

    public boolean parse(String... args) {

        if(args == null || args.length == 0) {
            throw new IllegalArgumentException("Should provide at least one argument");
        }

        String[] actualArgs = {"", "", "", ""};
        System.arraycopy(args, 0, actualArgs, 0, args.length);

        for(int i = 0; i < actualArgs.length; i++)
            params[i].set(actualArgs[i]);
        return !hasErrors();
    }

}

abstract class Parameter<T> {

    private String error;
    protected void setError(String error) {
        this.error = error;
    }

    public String getError() { return error; }

    public boolean isValid() { return error == null; }

    public abstract void set(String argument);

    protected T value;
    protected void setValue(T value) {
        this.value = value;

    }

    public T get() {
        if(isValid()) return value;
        return null;
    }
}

class PortParameter extends Parameter<Integer> {

    private int defaultPort;
    public PortParameter(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    @Override
    public void set(String argument) {
        try {
            if (argument == null || argument.equals(""))
                setValue(defaultPort);
            else
                setValue(Integer.parseInt(argument));
        }
        catch (NumberFormatException exc) {
            setError("Invalid port value. Details: " + exc);
        }
    }

}

class InstrumenterParameter extends Parameter<BehaviorInstrumenter> {

    @Override
    public void set(String argument) {

        if(argument == null || argument.equals("")) {
            argument = "invocation";
        }

        argument = argument.trim();
        String name = new StringBuilder("eu.stamp_project.dissector.agent.")
                .append(Character.toUpperCase(argument.charAt(0)))
                .append(argument.substring(1))
                .append("Instrumenter")
                .toString();

        try {
            Class clazz = Class.forName(name);
            if(BehaviorInstrumenter.class.isAssignableFrom(clazz)) {
                setValue((BehaviorInstrumenter) clazz.newInstance());
            }
            else {
                setError(String.format("Invalid instrumentation: %s. The class %s does not implement MethodInstrumenter", argument, clazz.getName()));
            }
        }
        catch(ClassNotFoundException  exc) {
            setError(String.format("Invalid instrumentation: %s. Class %s does not exist", argument, name));
        }
        catch(InstantiationException exc) {
            setError(String.format("Invalid instrumentation: %s. Could not create an instance of %s", argument, name));
        }
        catch(IllegalAccessException exc) {
            setError(String.format("Invalid instrumentation: %s. Class %s is not accessible", argument, name));
        }
    }
}


class FileLoggerParameter extends Parameter<FileLogger> {


    //Receives the folder where the log should be placed
    public void set(String folder) {

        if(folder == null || folder.equals(""))
            folder = "./";

        File log = new File(folder, getFileName());
        String absolutePath = log.getAbsolutePath();

        if(log.exists()) {
            if(!log.isFile()) {
                setError("Log path " + absolutePath + " is not a file.");
                return;
            }
            if(!log.canWrite()) {
                setError("Cannot write to log file " + absolutePath);
                return;
            }
        }
        else {
            try {
                if(!log.createNewFile()) {
                    setError("Cannot create log file in: " + absolutePath);
                }
            }
            catch (IOException exc) {
                setError("Cannot create log file in: " + absolutePath + ". Details: " + exc);
                return;
            }
        }
        try {
            setValue(new FileLogger(log));
        }
        catch(IOException exc) {
            setError("Error while creating the file logger instance. Details: " + exc);
        }
    }

    public String getFileName() {
        return "dissector-" + getLogSuffix() + ".log";
    }

    public static String getLogSuffix() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat( "yyyyMMddHHmmssSSS");
        return format.format(date);
    }

}

class InputFileParameter extends Parameter<File> {

    public void set(String argument) {
        //It is so tempting to add a list of predicates here :)
        // and some overcomplicated validation mechanism
        File input = new File(argument);
        if(!input.exists()) {
            setError("Input file " + argument + " does not exist.");
            return;
        }
        if(!input.isFile()) {
            setError("Input " + argument + " is not a file");
            return;
        }
        if(!input.canRead()) {
            setError("Can not read input file " + argument);
            return;
        }

        setValue(isValid()?input:null);
    }

}

