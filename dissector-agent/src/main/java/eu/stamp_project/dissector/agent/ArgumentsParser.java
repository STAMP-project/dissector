package eu.stamp_project.dissector.agent;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArgumentsParser {
    // Favor a tailored parser to keep the agent jar as small as possible
    // and to avoid license conflicts/

    private InputFileParameter inputFileParameter = new InputFileParameter();
    private InstrumenterParameter instrumenterParameter = new InstrumenterParameter();
    private PortParameter portParameter = new PortParameter();
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

    public MethodInstrumenter getInstrumenter() {
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

    public boolean parse(String args) {
        String[] values = args.split(":");
        String[] actualArgs = {"", "", "", ""};
        System.arraycopy(values, 0, actualArgs, 0, values.length);
        return parse(actualArgs);
    }

    public boolean parse(String... args) {
        for(int i = 0; i < args.length; i++)
            params[i].set(args[i]);
        return !hasErrors();
    }

}

abstract class Parameter<T> {

    private String error;
    protected void setError(String error) {
        this.error = error;
    }

    public String getError() { return error; }

    public boolean isValid() { return error != null; }

    public abstract void set(String argument);

    private T value;
    protected void setValue(T value) {
        this.value = value;

    }

    public T get() {
        if(isValid()) return value;
        return null;
    }
}

class PortParameter extends Parameter<Integer> {

    @Override
    public void set(String argument) {
        try {
            setValue(Integer.parseInt(argument));
        }
        catch (NumberFormatException exc) {
            setError("Invalid port value. Details: " + exc);
        }
    }

}

class InstrumenterParameter extends Parameter<MethodInstrumenter> {

    @Override
    public void set(String argument) {
        argument = argument.trim();
        String name = new StringBuilder("eu.stamp_project.dissector.agent.")
                .append(Character.toUpperCase(argument.charAt(0)))
                .append(argument.substring(1))
                .append("Instrumenter")
                .toString();

        try {
            Class clazz = Class.forName(name);
            if(!MethodInstrumenter.class.isAssignableFrom(clazz)) {
                setValue((MethodInstrumenter) clazz.newInstance());
            }
            else {
                setError(String.format("Invalid instrumentation: %s. The class %s does not implement MethodInstrumenter", argument, clazz.getName()));
            }
        }
        catch(ClassNotFoundException  exc) {
            setError(String.format("Invalid instrumentation: %s. Class %s does not exists", argument, name));
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


    public void set(String argument) {
        if(argument == null ||  argument.equals("")) {
            argument = getDefaultPath();
        }
        File log = new File(argument);
        if(log.exists()) {
            if(!log.isFile()) {
                setError("Log path " + argument + " is not a file.");
                return;
            }
            if(!log.canWrite()) {
                setError("Can not write to log file " + argument);
                return;
            }
        }
        else {
            try {
                if(!log.createNewFile()) {
                    setError("Can not create log file in: " + argument);
                }
            }
            catch (IOException exc) {
                setError("Can not create log file in: " + argument + ". Details: " + exc.getMessage());
                return;
            }
        }
        setValue(new FileLogger(log));
    }

    public String getDefaultPath() {
        return "./dissector-" + getLogSuffix() + ".log";
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

