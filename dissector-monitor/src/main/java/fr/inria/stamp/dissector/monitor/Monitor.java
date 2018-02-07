package fr.inria.stamp.dissector.monitor;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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

            getLog().info("Gathering target methods");

            MethodSet methods = getTargets();

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
        catch(NotFoundException exc) {
            throw new MojoExecutionException("Test classes could not be inspected. Details: " + exc.getMessage());
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

    private Set<String> getMethodsFromMutationFile() throws IOException {
        Set<String> classificationsOfInterest = new HashSet<>(); //TODO: Allow to configure this
        classificationsOfInterest.add("pseudo-tested");
        classificationsOfInterest.add("partially-tested");
        return getMethodsFromMutationFile(classificationsOfInterest);
    }

    private MethodSet getTargets() throws IOException, NotFoundException{

        Set<String> targets = getMethodsFromMutationFile();
        Set<String> testMethods = getTestMethods();
        targets.addAll(testMethods);

        return new MethodSet(new ArrayList<>(targets), testMethods);
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

    private Set<String> getTestMethods() throws IOException, NotFoundException {

        String rootFolder = _project.getBuild().getTestOutputDirectory();
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(rootFolder);
        TestMethodCollector collector = new TestMethodCollector(pool);

        Files.walkFileTree(Paths.get(rootFolder), new FileVisitor<Path>() {

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                String filePath = file.toString();
                String extension = ".class";
                if(!filePath.endsWith(extension)) return FileVisitResult.CONTINUE; //Using this instead of Path's capabilities due to an odd behavior in Path endsWith
                String className = filePath.substring(rootFolder.length() + 1, filePath.length() - extension.length()).replace('/', '.');
                collector.collectFrom(className);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)  {
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path dir, IOException exc)  {
                return FileVisitResult.CONTINUE;
            }

        });

        return collector.getCollectecMethods();

    }

}
