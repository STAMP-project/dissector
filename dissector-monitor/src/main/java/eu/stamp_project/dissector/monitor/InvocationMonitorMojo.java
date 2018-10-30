package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class InvocationMonitorMojo extends DynamicDissectorMojo {

    private final Pattern logPattern = Pattern.compile("\\[\\[D\\]\\[(?<type>.):(?<method>\\d+):(?<thread>\\d+):(?<depth>\\d+)\\]\\]");

    @Override
    protected String instrumenter() { return "invocation"; }

    @Override
    protected void processLine(String line) throws MojoExecutionException {
        Matcher match = logPattern.matcher(line);
        if (!match.matches()) return;
        getLog().debug(line);
        String action = match.group("type");
        int thread = Integer.parseInt(match.group("thread"));
        int method = Integer.parseInt(match.group("method"));
        int depth = Integer.parseInt(match.group("depth"));

        switch (action) { //In this case, the switch-case is more readable than an if
            case ">":
                onMethodEnter(thread, method, depth);
                break;
            case "<":
                onMethodExit(thread, method, depth);
                break;
            default:
                getLog().warn("Unknown action: " + action);
        }
    }


    protected abstract void onMethodExit(int thread, int method, int depth);

    protected abstract void onMethodEnter(int thread, int method, int depth);
}
