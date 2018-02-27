package fr.inria.stamp.dissector.monitor;

import com.google.gson.Gson;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.util.Set;

@Mojo(name = "count-tests")
public class TestCounterMojo extends DissectorMojo {

    @Parameter(property = "output", defaultValue = "${project.build.directory}/tests.json")
    private File _output;

    public File getOutput() { return  _output; }

    public void setOutput(File output) {
        _output = output;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Set<TestInfo> tests = getTestMethods();
            Gson gson = new Gson();
            try(FileWriter writer = new FileWriter(_output)) {
                gson.toJson(tests, writer);
            }
        }
        catch (Throwable exc) {
            throw new MojoFailureException(exc.getMessage());
        }
    }

}
