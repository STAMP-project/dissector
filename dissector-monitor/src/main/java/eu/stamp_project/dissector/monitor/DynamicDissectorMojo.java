package eu.stamp_project.dissector.monitor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

public abstract class DynamicDissectorMojo extends DissectorMojo {

    /**
     * File with the list of methods to pass to the agent
     */
    @Parameter(property = "methodList", defaultValue = "${project.build.directory}/methods.input.txt")
    protected File _methodList;

    public File getMethodList() {
        return _methodList;
    }

    public void setMethodList(File methodList) {
        _methodList = methodList;
    }

    /**
     * Path to the JAR file containing the agent implementation.
     * If none is given then, the JAR is automatically injected
     * form the resources.
     */
    @Parameter(property = "agentJar")
    protected String _agentJar;

    public String getAgentJar() {
        return _agentJar;
    }

    public void setAgentJar(String agentJar) {
        _agentJar = agentJar;
    }

    /**
     * Port to communicate with the agent.
     */
    @Parameter(property = "port", defaultValue = "2112")
    protected int _port;

    public int getPort() {
        return _port;
    }

    public void setPort(int port) {
        _port = port;
    }

    /**
     * Maven profile name to run the tests.
     */
    @Parameter(property = "testingProfile", defaultValue = "stamp-dissector")
    protected String _testingProfile;

    public String getTestingProfile() {
        return _testingProfile;
    }

    public void setTestingProfile(String testingProfile) {
        _testingProfile = testingProfile;
    }

    /**
     * Tells whether to inject agent arguments and testing profile to the 'mvn test' command.
     */
    @Parameter(property = "injectTestArgs", defaultValue = "true")
    protected boolean _injectTestArgs;

    public boolean getInjectTestArgs() {
        return _injectTestArgs;
    }

    public void setInjectArgs(boolean injectTestArgs) {
        _injectTestArgs = injectTestArgs;
    }

    /**
     * Path to a JSON file with the methods to involve in the execution
     */

    @Parameter(property = "methodsOfInterest", defaultValue = "${project.build.directory}/methods.json")
    protected File _methodsOfInterest;

    public File getMethodsOfInterest() {
        return _methodsOfInterest;
    }

    public void setMethodsOfInterest(File methodsOfInterest) {
        _methodsOfInterest = methodsOfInterest;
    }

    /**
     * If given, the methodsOfInterest parameter will be filtered by a classification field
     */
    @Parameter(property = "classificationsOfInterest")
    protected Set<String> _classificationsOfInterest;

    public Set<String> getClassificationsOfInterest() {
        if(_classificationsOfInterest == null)
            return Collections.EMPTY_SET;
        return _classificationsOfInterest;
    }

    public void setClassificationsOfInterest(Set<String> classificationsOfInterest) {
        this._classificationsOfInterest = classificationsOfInterest;
    }

    protected List<String> getTargetMethodsFromFile() throws IOException {

        Gson gson = new Gson();

        FileReader reader = new FileReader(_methodsOfInterest);
        JsonObject root = gson.fromJson(reader, JsonObject.class);
        JsonArray arrayOfEntries = (root.isJsonArray())?  root.getAsJsonArray() : root.getAsJsonArray("methods");

        List<String> targetMethods = new LinkedList<>();

        //ARRGGHH Java!
        // The best way to get from Iterator to Stream
        // https://stackoverflow.com/questions/24511052/how-to-convert-an-iterator-to-a-stream
        Stream<JsonElement> elements = StreamSupport.stream(Spliterators.spliteratorUnknownSize(arrayOfEntries.iterator(), Spliterator.ORDERED), false);

        Stream<JsonObject> entries = elements.map(JsonElement::getAsJsonObject);

        if(!_classificationsOfInterest.isEmpty()) {

            entries = entries.filter(entry ->
                    entry.has("classification") &&
                            _classificationsOfInterest.contains(entry.get("classification").getAsString()));
        }

        return entries.map(entry ->
                String.format("%s/%s/%s%s",
                        entry.get("package").getAsString(),
                        entry.get("class").getAsString(),
                        entry.get("name").getAsString(),
                        entry.get("description").getAsString()
                )).collect(Collectors.toList());
    }

    protected MethodSet targetsForTheAgent;

    protected MethodSet gatherTargetsForTheAgent() throws IOException {
        return new MethodSet((getTargetMethodsFromFile()));
    }

    protected void prepareAgentTargets() throws MojoExecutionException {
        try {
            targetsForTheAgent = gatherTargetsForTheAgent();
            targetsForTheAgent.save(_methodList);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Failed to save the list of methods", exc);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Obtaning the list of methods for the agent");
        prepareAgentTargets();

        getLog().info("Preparing agent execution");
        prepareExecution();

        getLog().info("Executing the tests");
        executeTestCommand();

        getLog().info("Finishing execution");
        finishExecution();
    }

    protected  void executeTestCommand() throws MojoExecutionException {
        Process testProcess = null;
        try {
            List<String> command = getTestCommand();
            getLog().debug("Executing: " + command.stream().collect(joining(" ")));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
            testProcess= processBuilder.start();

            InputStreamReader inputReader = new InputStreamReader(testProcess.getErrorStream());
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            monitorProcess(bufferedReader.lines());

        }
        catch (IOException exc) {
            throw new MojoExecutionException("Failed to execute the test process", exc);
        }
        finally {
            if(testProcess != null && testProcess.isAlive())
                testProcess.destroyForcibly();
        }

    }

    private void monitorProcess(Stream<String> incomming) throws MojoExecutionException {

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



    protected List<String> getTestCommand() throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("test");

        if(!_injectTestArgs)
            return command;

        if(hasValue(_testingProfile))
            command.add("-P" +_testingProfile);

        command.add(getAgentArgs());
        return command;
    }

    protected String getAgentArgs() throws MojoExecutionException {
        //TODO: Not handling the port yet
        return String.format("-DargLine=-javaagent:%s=%s", getAgentJarPath(), _methodList.getAbsolutePath());
    }

    protected String getAgentJarPath() throws MojoExecutionException {
        if (hasValue(_agentJar))
            return _agentJar;

        try {
            InputStream agentJarFromResources = getClass()
                    .getClassLoader()
                    .getResourceAsStream("dissector-agent-jar-with-dependencies.jar");

            File jarFile = File.createTempFile("__stamp__", "__dissector_agent__");
            FileOutputStream jarFileOutput = new FileOutputStream(jarFile);
            copy(agentJarFromResources, jarFileOutput);
            jarFileOutput.close();

            return jarFile.getAbsolutePath();
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Failed to get the agent JAR",exc);
        }
    }

    // AARGGHH JAVA
    //https://stackoverflow.com/questions/4919690/how-to-read-one-stream-into-another
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0x1000];
        int length;
        while((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }

    protected abstract void prepareExecution() throws MojoExecutionException;

    protected abstract void onMethodExit(int thread, int method, int depth);

    protected abstract void onMethodEnter(int thread, int method, int depth);

    protected abstract void finishExecution() throws MojoExecutionException;
}
