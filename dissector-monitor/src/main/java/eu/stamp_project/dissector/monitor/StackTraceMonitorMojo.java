package eu.stamp_project.dissector.monitor;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Mojo(name="stack-trace")
public class StackTraceMonitorMojo extends DynamicDissectorMojo {

    private Pattern stackTracePattern = Pattern.compile("\\[\\[D\\](?<method>\\d+):(<[^:]+:[^:]+:[^:]+:[^:]+>)+\\]");

    @Override
    protected String instrumenter() { return "stack"; }

    @Override
    protected void prepareExecution() throws MojoExecutionException {

    }

    @Override
    protected void monitorProcess(Stream<String> output) throws MojoExecutionException {
        output
                .map(line -> stackTracePattern.matcher(line))
                .filter(Matcher::matches)
                .forEach(match -> getLog().debug(match.group(0)));
        ;
    }

    @Override
    protected void finishExecution() throws MojoExecutionException {

    }
}
