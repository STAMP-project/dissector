package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class InvocationMonitorMojo extends DynamicDissectorMojo {

    @Override
    protected String instrumenter() { return "invocation"; }

    @Override
    protected void monitorProcess(Stream<String> incomming) throws MojoExecutionException {

        final Pattern logPattern = Pattern.compile("\\[\\[D\\]\\[(?<type>.):(?<method>\\d+):(?<thread>\\d+):(?<depth>\\d+)\\]\\]");

        incomming.forEach(line -> {
            getLog().debug(line);

            Matcher match = logPattern.matcher(line);
            if (!match.matches()) return;

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
        });
    }


    protected abstract void onMethodExit(int thread, int method, int depth);

    protected abstract void onMethodEnter(int thread, int method, int depth);
}
