package eu.stamp_project.dissector.monitor;

import com.google.gson.Gson;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "distance-to-test")
public class DistanceToTestMojo extends InvocationMonitorMojo implements TestMethodAwareMojo {

    @Parameter(property = "output", defaultValue = "${project.build.directory}/stack-distance.json")
    protected File _output;

    public File getOutput() {
        return _output;
    }

    public void setOutput(File output) {
        _output = output;
    }

    private MethodSet targetMethods;

    private ProcessEmulator emulator;


    @Override
    protected void prepareExecution() throws MojoExecutionException {
        emulator = new ProcessEmulator(targetsForTheAgent);
    }

    @Override
    protected MethodSet gatherTargetsForTheAgent() throws IOException {
        Set<String> methods = new HashSet<>(getTargetMethodsFromFile());
        Set<String> testMethods = getTestMethods();
        methods.addAll(testMethods);

        getLog().debug("Target methods: " + methods.size());
        getLog().debug("Test methods: " + testMethods.size());
        return new MethodSet(new ArrayList<>(methods), testMethods);
    }

    @Override
    protected void onMethodExit(int thread, int method, int depth) {
        if(!emulator.isCorrupt()) {
            emulator.exit(thread, method, depth);
        }
    }

    @Override
    protected void onMethodEnter(int thread, int method, int depth) {
        if (!emulator.isCorrupt()) {
            emulator.enter(thread, method, depth);
        }
    }

    @Override
    protected void finishExecution() throws MojoExecutionException {
        if (emulator.isCorrupt())
            throw new MojoExecutionException("Emulation produced a corrupt state");
        List<MethodEntry>  report = emulator.getReport();
        if(report.isEmpty())
            getLog().warn("Emulation report was empty");
        saveReport(report);
    }

    protected void saveReport(List<MethodEntry> report)  throws MojoExecutionException {
        try {
            Gson gson = new Gson();
            try (FileWriter writer = new FileWriter(_output)) {
                gson.toJson(report, writer);
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while saving the report. Details: " + exc.getMessage(), exc);
        }
    }
}
