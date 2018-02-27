package fr.inria.stamp.dissector.monitor;

import com.google.gson.Gson;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ExecutorMojo extends DissectorMojo {

    @Parameter(property = "methodList", defaultValue = "${project.build.directory}/methods.input.txt")
    protected File _methodList;

    public File getMethodList() {
        return _methodList;
    }

    public void setMethodList(File methodList) {
        _methodList = methodList;
    }

    @Parameter(property = "output", defaultValue = "${project.build.directory}/distances.json")
    private File _output;

    public File getOutput() { return  _output; }

    public void setOutput(File output) {
        _output = output;
    }


    protected Set<String> getTestMethodNames() throws MojoExecutionException {
        return getTestMethods().stream().map(t -> t.name).collect(Collectors.toSet());
    }

    protected MethodSet getMethodSet() throws MojoExecutionException {
        return new MethodSet(getTargetMethods(), getTestMethodNames());
    }

    protected abstract List<String> getTargetMethods() throws MojoExecutionException;


    protected List<MethodEntry> executeProcess(final Stream<String> feed, final ProcessEmulator emulator) throws MojoFailureException {
        final Pattern logPattern = Pattern.compile("\\[\\[D\\]\\[(?<type>.):(?<method>\\d+):(?<thread>\\d+):(?<depth>\\d+)\\]\\]");
        //final ProcessEmulator emulator = new ProcessEmulator((getMethodSet()));

        feed.forEach((String line) -> {
            getLog().debug(line);

            Matcher match = logPattern.matcher(line);
            if (!match.matches()) return;

            String action = match.group("type");
            int thread = Integer.parseInt(match.group("thread"));
            int method = Integer.parseInt(match.group("method"));
            int depth = Integer.parseInt(match.group("depth"));

            if (action.equals(">"))
                emulator.enter(thread, method, depth);
            else if (action.equals("<"))
                emulator.exit(thread, method, depth);
        });
        if (emulator.isCorrupt())
            throw new MojoFailureException("Emulation process was corrupt");

        return emulator.getReport();
    }


    protected void saveReport(List<MethodEntry> report)  throws MojoExecutionException {
        try {
            Gson gson = new Gson();
            try (FileWriter writer = new FileWriter(_output)) {
                gson.toJson(report, writer);
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("An error occurred wjle saving the report. Details: " + exc.getMessage(), exc);
        }
    }

    protected void executeAndSave(Stream<String> feed, ProcessEmulator emulator) throws MojoExecutionException, MojoFailureException {
        List<MethodEntry> report = executeProcess(feed, emulator);
        getLog().info("Saving the report");
        if(report.size() == 0)
            getLog().warn("Final report was empty");
        saveReport(report);
    }

    protected void executeAndSave(Stream<String> feed) throws MojoExecutionException, MojoFailureException {
        executeAndSave(feed, new ProcessEmulator(getMethodSet()));
    }


}
