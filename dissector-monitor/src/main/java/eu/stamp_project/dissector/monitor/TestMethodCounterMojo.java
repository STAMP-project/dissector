package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Set;

@Mojo(name = "count-tests")
public class TestMethodCounterMojo extends DissectorMojo implements TestMethodAwareMojo, JsonReporterMojo<Set<String>> {

    @Parameter(property = "output", defaultValue = "${project.build.directory}/test-methods.json")
    private File _output;

    public File getOutput() { return  _output; }

    public void setOutput(File output) {
        _output = output;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<String> methods = getTestMethods();
        getLog().info(methods.size() + " test methods collected");
        saveReport(methods);
    }

    public Set<String> buildReport() {
        return getTestMethods();
    }

}
