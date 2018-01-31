package fr.inria.stamp.dissector.monitor;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javassist.bytecode.Descriptor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    //TODO: Find the jar with all dependencies
    //TODO: There was something with ${maven.dependency.fr.inria.stamp....} that didn't worked
    @Parameter(property = "agentJar", defaultValue = "${settings.localRepository}/fr/inria/stamp/dissector-agent/1.0-SNAPSHOT/dissector-agent-1.0-SNAPSHOT-jar-with-dependencies.jar")
    private String _agentJar;

    public String getAgentJar() { return _agentJar; }

    public void setAgentJar(String agentJar) { _agentJar = agentJar; }

    @Parameter(property = "testingProfile", defaultValue = "${dissector.profile}")
    private String _testingProfile;

    public String getTestingProfile() { return _testingProfile; }

    public void setTestingProfile(String testingProfile) { _testingProfile = testingProfile; }

    @Parameter(property = "injectArgs", defaultValue = "true")
    private boolean _injectArgs;

    public boolean getInjectArgs() { return _injectArgs; }

    public void setInjectArgs(boolean injectArgs) { _injectArgs = injectArgs; }

    @Parameter(property = "output", defaultValue = "${project.build.directory}/distances.json")
    private File _output;

    public File getOutput() { return  _output; }

    public void setOutput(File output) {
        _output = output;
    }

    //-javaagent:${settings.localRepository}/fr/inria/stamp/dissector-agent/1.0-SNAPSHOT/dissector-agent-1.0-SNAPSHOT-jar-with-dependencies.jar=${project.build.directory}/methods.input.txt


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Process testProcess = null;
        try {

            getLog().info("Loading mutation file");

            MethodSet methods = methodsFromMutationFile(_mutationFile);

            getLog().info("Saving method list");
            methods.save(_methodList);

            ProcessEmulator emulator = new ProcessEmulator(methods);

            getLog().info("profile " + _testingProfile);

            ProcessBuilder builder = new ProcessBuilder(getTestCommand());

            getLog().info("Testing with command:");
            getLog().info(builder.command().stream().collect(Collectors.joining(" ")));

            builder.redirectError(ProcessBuilder.Redirect.PIPE);

            testProcess = builder.start();

            processErrorStream(testProcess.getErrorStream(), emulator);

            testProcess.waitFor();

            if(emulator.isCorrupt()) {
                getLog().warn("Testing process produced a corrupt stack emulation");
            }

            List<MethodEntry> report = emulator.getReport();
            if(report.size() == 0)
                getLog().warn("Final report was empty");

            getLog().info("Saving final report");

            saveReport(report);

        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not start test process. Details: " +  exc.getMessage());
        }
        catch (InterruptedException exc) {
            throw new MojoExecutionException("Testing process was interrupted. Details: " + exc.getMessage());
        }

        finally {
            if(testProcess != null && testProcess.isAlive()) {
                testProcess.destroy();
            }
        }

    }

    private List<String> getTestCommand() {

        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("test");

        if(!_injectArgs) return command;

        if(_testingProfile != null && !_testingProfile.equals(""))
            command.add("-P" + _testingProfile);

        command.add(String.format("-DargLine=-javaagent:%s=%s", _agentJar, _methodList.getAbsolutePath()));
        return command;

    }

    //TODO: Take this method out of this class?
    private void processErrorStream(InputStream input, ProcessEmulator emulator) throws MojoExecutionException {

        Pattern logPattern = Pattern.compile("\\[\\[D\\]\\[(?<type>.):(?<method>\\d+):(?<thread>\\d+):(?<depth>\\d+)\\]\\]");

        try {
            if(input == null) {
                getLog().error("input is null");
            }

            InputStreamReader inputReader = new InputStreamReader(input);
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String line = null;

            while ((line = bufferedReader.readLine()) != null) {

                getLog().debug(line);

                Matcher match = logPattern.matcher(line);
                if(!match.matches()) continue;



                String action = match.group("type");
                int thread = Integer.parseInt(match.group("thread"));
                int method = Integer.parseInt(match.group("method"));
                int depth = Integer.parseInt(match.group("depth"));

                if(action.equals(">"))
                    emulator.enter(thread, method, depth);
                else if(action.equals("<"))
                    emulator.exit(thread, method, depth);
            }
        }
        catch (IOException exc) {
            MojoExecutionException next = new MojoExecutionException("Failed to read the testing output");
            next.addSuppressed(exc);
            throw next;
        }
    }


    private void saveReport(List<MethodEntry> report)  throws IOException {
        Gson gson = new Gson();
        try(FileWriter writer = new FileWriter(_output)) {
            gson.toJson(report, writer);
        }
    }



    private MethodSet methodsFromMutationFile(File mutationFile) throws IOException {
        Set<String> classificationsOfInterest = new HashSet<>(); //TODO: Allow to configure this
        classificationsOfInterest.add("pseudo-tested");
        classificationsOfInterest.add("partially-tested");
        return methodsFromMutationFile(mutationFile, classificationsOfInterest);
    }


    private MethodSet methodsFromMutationFile(File mutationFile, Set<String> classificationsOfInterest) throws IOException {

        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(new FileReader(mutationFile)).getAsJsonObject();

        Set<String> methods = new HashSet<>();
        Set<String> tests = new HashSet<>();

        //Valid test name
        Pattern testNamePattern = Pattern.compile("^(?<method>[^\\[\\]]+)(\\[.*\\])?\\(.*\\)$", Pattern.DOTALL);

        for (JsonElement element : root.getAsJsonArray("methods")) {
            JsonObject methodObj = element.getAsJsonObject();

            if(!classificationsOfInterest.contains(methodObj.get("classification").getAsString())) continue;

            String packageName = methodObj.get("package").getAsString().replace("/", ".");
            String className = methodObj.get("class").getAsString();
            String methoName = methodObj.get("name").getAsString();
            String signature = Descriptor.toString(methodObj.get("description").getAsString());
            methods.add(String.format("%s.%s.%s%s", packageName, className, methoName, signature));

            for(JsonElement testNameElement : methodObj.getAsJsonArray("tests")) {

                String testName = testNameElement.getAsString();
                Matcher match = testNamePattern.matcher(testName);
                if(!match.matches()) {
                    getLog().warn("Ignoring wrong test name: " + testName);
                    continue;
                }
                String finalTestName = match.group("method") + "()";
                methods.add(finalTestName);
                tests.add(finalTestName);
            }

        }

        return new MethodSet(new ArrayList<>(methods), tests);
    }

}
