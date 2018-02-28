package fr.inria.stamp.dissector.monitor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


@Mojo(name = "from-trace")
public class DebugTraceMonitor extends ExecutorMojo {

    @Parameter(property = "traceFile", defaultValue = "${project.build.directory}/method.trace.txt")
    private File _traceFile;

    public File getTraceFile() {
        return _traceFile;
    }

    public void setTraceFile(File traceFile) {
        _traceFile = traceFile;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try (Stream<String> feed = Files.lines(_traceFile.toPath())) {
            executeAndSave(feed);
        } catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while reading the list of methods.", exc);

        }
    }

    @Override
    protected MethodSet getMethodSet() throws MojoExecutionException {
        return new MethodSet(getTargetMethods(), getTestMethods());
    }

    private List<String> getTargetMethods() throws MojoExecutionException {
        try {
            ArrayList<String> result = new ArrayList<>();
            try (FileReader reader = new FileReader(_methodList)) {
                BufferedReader br = new BufferedReader(reader);
                String line = null;
                while ((line = br.readLine()) != null)
                    result.add(line);
            }
            return result;
        } catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while reading the list of methods.", exc);
        }
    }

}
