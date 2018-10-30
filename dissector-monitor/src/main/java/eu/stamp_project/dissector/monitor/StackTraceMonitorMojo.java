package eu.stamp_project.dissector.monitor;


import eu.stamp_project.dissector.monitor.reporting.MethodTracesEntry;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name="stack-trace")
public class StackTraceMonitorMojo extends DynamicDissectorMojo implements JsonReporterMojo<MethodTracesEntry[]> {

    private Pattern stackTracePattern = Pattern.compile("\\[\\[D\\]\\[(?<method>\\d+):((<[^:]+:[^:]+:[^:]+:-?\\d+>)+)\\]\\]");
    private Pattern stackTraceElementPattern = Pattern.compile("<(?<file>[^:]+):(?<class>[^:]+):(?<method>[^:]+):(?<line>-?\\d+)>");

    @Parameter(property = "output", defaultValue = "${project.build.directory}/stack-traces.json")
    protected File _output;

    public File getOutput() {
        return _output;
    }

    public void setOutput(File output) {
        _output = output;
    }

    @Override
    protected String instrumenter() { return "stack"; }

    @Override
    protected void prepareExecution() throws MojoExecutionException { }

    //Keep only the different traces
    private Set<String> collectedTraces = new HashSet<>();

    @Override
    protected void processLine(String line) throws MojoExecutionException {
        Matcher match = stackTracePattern.matcher(line);
        if(!match.matches()) {
            return;
        }
        collectedTraces.add(line);
        getLog().debug(line);
    }

    @Override
    protected void finishExecution() throws MojoExecutionException {
        buildAndSaveReport();
    }

    public MethodTracesEntry[] buildReport() {
        MethodTracesEntry[] entries = targetsForTheAgent.getMethods()
                .stream()
                .map(MethodTracesEntry::new)
                .toArray(MethodTracesEntry[]::new);

        for(String trace : collectedTraces) {
            Matcher match = stackTracePattern.matcher(trace);
            match.matches();
            int index = Integer.parseInt(match.group("method"));
            if(index >= targetsForTheAgent.size()) continue; //Unlikely, but...
            entries[index].addTrace(parse(match.group(2))); //?
        }
        return entries;
    }

    private StackTraceElement[] parse(String trace) {
        ArrayList<StackTraceElement> result = new ArrayList<>();
        Matcher matcher = stackTraceElementPattern.matcher(trace);
        while(matcher.find()) {

            result.add(
                    new StackTraceElement(
                            matcher.group("class"),
                            matcher.group("method"),
                            matcher.group("file"),
                            Integer.parseInt(matcher.group("line"))
                    )
            );
        }
        return result.stream().toArray(StackTraceElement[]::new);
    }
}
