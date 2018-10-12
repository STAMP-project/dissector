package eu.stamp_project.dissector.monitor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public abstract class AgentExecutionMojo extends BaseDissectorMojo{

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Preparing agent execution");
        prepareExecution();

        getLog().info("Saving the list of methods to target");
        saveMethodList();

        getLog().info("Executing the tests");
        executeTestCommand();

    }

    protected  void executeTestCommand() throws MojoExecutionException {
        Process testProcess = null;
        try {
            List<String> command = getTestCommand();
            getLog().debug("Executing: " + command.stream().collect(joining("")));

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

    private void monitorProcess(Stream<String> lines) {

        final Pattern logPattern = Pattern.compile("\\[\\[D\\]\\[(?<type>.):(?<method>\\d+):(?<thread>\\d+):(?<depth>\\d+)\\]\\]");
        //final ProcessEmulator emulator = new ProcessEmulator((getMethodSet()));

        lines.forEach((String line) -> {
            getLog().debug(line);

            Matcher match = logPattern.matcher(line);
            if (!match.matches()) return;

            String action = match.group("type");
            int thread = Integer.parseInt(match.group("thread"));
            int method = Integer.parseInt(match.group("method"));
            int depth = Integer.parseInt(match.group("depth"));

            if (action.equals(">"))
                onMethodEnter(thread, method, depth);

            else if (action.equals("<"))
                onMethodExit(thread, method, depth);
        });
    }

    protected abstract void onMethodExit(int thread, int method, int depth);

    protected abstract void onMethodEnter(int thread, int method, int depth);

    protected void saveMethodList() throws MojoExecutionException {
        MethodSet methods = getTargetMethods();
        try {
            methods.save(_methodList);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Failed to save the list of methods", exc);
        }
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
                    .getResourceAsStream("dissector-agent.jar");

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

    private boolean hasValue(String parameter) {
        return parameter != null && !parameter.equals("");
    }


    /**
     * Actions to be executed by the extending classes to prepare the agent execution.
     */
    protected abstract void prepareExecution();

    protected abstract MethodSet getTargetMethods();
}
