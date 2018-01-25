package fr.inria.stamp.dissector.monitor;


import com.google.gson.Gson;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO: Automatically insert the agent.
//TODO: Inspect the project to gather methods

@Mojo(name = "execute")
public class Monitor extends AbstractMojo {

    @Parameter(defaultValue="${project}")
    private MavenProject _project;

    public MavenProject getProject() {
        return _project;
    }

    public void setProject(MavenProject project) {
        _project = project;
    }

    //TODO: If no mutation file is given, extract the information from the project

    @Parameter(property="mutationFile", defaultValue = "${project.build.directory}/mutations.json")
    private File _mutationFile;

    public File getMutationFile() {
        return _mutationFile;
    }

    public void setMutationFile(File mutationFile) {
        _mutationFile = mutationFile;
    }

    @Parameter(property = "methodList", defaultValue = "${project.build.directory}/methods.input.txt")
    private File _methodList;

    public File getMethodList() {
        return _methodList;
    }

    public void setMethodList(File methodList) {
        _methodList = methodList;
    }

    @Parameter(property = "output", defaultValue = "${project.build.directory}/distances.json")
    File _output;

    public File getOutput() { return  _output; }

    public void setOutput(File output) {
        _output = output;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            getLog().info("Loading mutation file");

            MethodSet methods = MethodSet.fromMutationFile(_mutationFile);

            getLog().info("Saving method list");
            methods.save(_methodList);


            ProcessEmulator emulator = new ProcessEmulator(methods);

            getLog().info("Processing test output");

            ProcessBuilder builder = new ProcessBuilder("mvn", "test");

            builder.redirectError(ProcessBuilder.Redirect.PIPE);

            Process testProcess = builder.start();

            processErrorStream(testProcess.getErrorStream(), emulator);

            testProcess.waitFor();

            getLog().info("Saving final report");

            saveReport(emulator.getReport());

        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not start test process. Details: " +  exc.getMessage());
        }
        catch (InterruptedException exc) {
            throw new MojoExecutionException("Testing process was interrupted. Details: " + exc.getMessage());
        }

    }

    private void processErrorStream(InputStream input, ProcessEmulator emulator) {

        Pattern logPattern = Pattern.compile("\\[\\[D\\]\\[(?<type>.):(?<method>\\d+):(?<thread>\\d+):(?<depth>\\d+)\\]");

        try {
            if(input == null) {
                getLog().error("input is null");
            }

            InputStreamReader inputReader = new InputStreamReader(input);
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                Matcher match = logPattern.matcher(line);
                if(!match.matches()) continue;

                if(match.group("type").equals(">"))
                    emulator.enter(Integer.parseInt(match.group("thread")),
                            Integer.parseInt(match.group("method")),
                            Integer.parseInt(match.group("depth")));

            }
        }
        catch (IOException exc) {
            getLog().error(exc);
        }
    }


    private void saveReport(List<MethodEntry> report)  throws IOException {
        Gson gson = new Gson();
        try(FileWriter writer = new FileWriter(_output)) {
            gson.toJson(report, writer);
        }
    }

}
