package eu.stamp_project.dissector.monitor;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//TODO: Automatically insert the agent.
//TODO: Inspect the project to gather methods

@Mojo(name = "execute")
public class Monitor extends ExecutorMojo {

    //TODO: If no mutation file is given, extract the information from the project

    @Parameter(property = "mutationFile", defaultValue = "${project.build.directory}/mutations.json")
    private File _mutationFile;

    public File getMutationFile() {
        return _mutationFile;
    }

    public void setMutationFile(File mutationFile) {
        _mutationFile = mutationFile;
    }

    //TODO: Find the jar with all dependencies
    //TODO: There was something with ${maven.dependency.fr.inria.stamp....} that didn't work
    @Parameter(property = "agentJar", defaultValue = "${settings.localRepository}/fr/inria/stamp/dissector-agent/1.0-SNAPSHOT/dissector-agent-1.0-SNAPSHOT-jar-with-dependencies.jar")
    private String _agentJar;

    public String getAgentJar() {
        return _agentJar;
    }

    public void setAgentJar(String agentJar) {
        _agentJar = agentJar;
    }

    @Parameter(property = "readFromStandardOutput", defaultValue = "false")
    private boolean _readFromStandardOutput = false;

    public boolean readFromStandardOutput() {
        return _readFromStandardOutput;
    }

    public void setReadFromStandardOutput(boolean value) {
        _readFromStandardOutput = value;
    }

    @Parameter(property = "testingProfile", defaultValue = "${dissector.profile}")
    //There is no need to put ${dissector.profile} we'll put just dissector, if there is a need to call from the command line, then -DtestingProfile
    private String _testingProfile;

    public String getTestingProfile() {
        return _testingProfile;
    }

    public void setTestingProfile(String testingProfile) {
        _testingProfile = testingProfile;
    }

    @Parameter(property = "injectArgs", defaultValue = "true")
    private boolean _injectArgs;

    public boolean getInjectArgs() {
        return _injectArgs;
    }

    public void setInjectArgs(boolean injectArgs) {
        _injectArgs = injectArgs;
    }


    //-javaagent:${settings.localRepository}/fr/inria/stamp/dissector-agent/1.0-SNAPSHOT/dissector-agent-1.0-SNAPSHOT-jar-with-dependencies.jar=${project.build.directory}/methods.input.txt


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Gathering target methods");
        MethodSet methods = getMethodSet();

        try {
            getLog().info("Saving method list");
            methods.save(_methodList);
        } catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while saving the method list.", exc);
        }

        execute(new ProcessEmulator(methods));
    }

    private void execute(ProcessEmulator emulator) throws MojoExecutionException, MojoFailureException {

        Process testProcess = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(getTestCommand());

            getLog().info("Testing with command:");
            getLog().info(builder.command().stream().collect(Collectors.joining(" ")));

            if (_readFromStandardOutput) {
                builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
            } else {
                builder.redirectError(ProcessBuilder.Redirect.PIPE);
            }

            testProcess = builder.start();
            InputStreamReader inputReader = new InputStreamReader(
                    _readFromStandardOutput ?
                            testProcess.getInputStream()
                            :
                            testProcess.getErrorStream());
            BufferedReader bufferedReader = new BufferedReader(inputReader);
            executeAndSave(bufferedReader.lines(), emulator);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while executing the testing process.", exc);

        }
        finally {
            if(testProcess != null && testProcess.isAlive())
                testProcess.destroyForcibly();
        }
    }

    private List<String> getTestCommand() {

        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("test");

        if (!_injectArgs) return command;

        if (_testingProfile != null && !_testingProfile.equals(""))
            command.add("-P" + _testingProfile);

        command.add(String.format("-DargLine=-javaagent:%s=%s", _agentJar, _methodList.getAbsolutePath()));
        return command;

    }

    @Override
    protected MethodSet getMethodSet() throws MojoExecutionException {
        Set<String> targets = getMethodsFromMutationFile();
        Set<String> tests = getTestMethods();
        targets.addAll(tests);
        getLog().debug("Target methods: " + targets.size());
        getLog().debug("Test methods: " + tests.size());
        return new MethodSet(new ArrayList<>(targets), tests);
    }

    private Set<String> getMethodsFromMutationFile() throws MojoExecutionException {
        try {
            Set<String> classificationsOfInterest = new HashSet<>(); //TODO: Allow to configure this
            classificationsOfInterest.add("pseudo-tested");
            classificationsOfInterest.add("partially-tested");
            return getMethodsFromMutationFile(classificationsOfInterest);
        }
        catch (IOException exc){
            throw new MojoExecutionException("An error occurred while reading the list of methods from the mutation report.", exc);
        }
    }

    private Set<String> getMethodsFromMutationFile(Set<String> classificationsOfInterest) throws IOException {
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(new FileReader(_mutationFile)).getAsJsonObject();

        Set<String> methods = new HashSet<>();

        for (JsonElement element : root.getAsJsonArray("methods")) {
            JsonObject methodObj = element.getAsJsonObject();

            if (!classificationsOfInterest.contains(methodObj.get("classification").getAsString())) continue;

            String packageName = methodObj.get("package").getAsString();
            String className = methodObj.get("class").getAsString();
            String methodName = methodObj.get("name").getAsString();
            String desc = methodObj.get("description").getAsString();


            methods.add(String.format("%s/%s/%s%s", packageName, className, methodName, desc));
        }

        return methods;
    }

}
